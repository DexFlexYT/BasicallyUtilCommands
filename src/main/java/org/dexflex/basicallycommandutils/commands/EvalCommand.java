package org.dexflex.basicallycommandutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EvalCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("eval")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("scale", IntegerArgumentType.integer(1))
                        .then(CommandManager.argument("expression", StringArgumentType.greedyString())
                                .executes(EvalCommand::execute)
                        )
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int scale = IntegerArgumentType.getInteger(context, "scale");
        String expression = StringArgumentType.getString(context, "expression");

        ExpressionEvaluator evaluator = new ExpressionEvaluator(context.getSource());
        double result = evaluator.evaluate(expression);
        int scaledResult = (int) (result * scale);

        context.getSource().sendFeedback(
                () -> Text.literal("Result: " + result + " (scaled: " + scaledResult + ")"),
                false
        );

        return scaledResult;
    }

    // ==================== Expression Evaluator ====================

    private static class ExpressionEvaluator {
        private final ServerCommandSource source;
        private final Tokenizer tokenizer;

        public ExpressionEvaluator(ServerCommandSource source) {
            this.source = source;
            this.tokenizer = new Tokenizer();
        }

        public double evaluate(String expression) throws CommandSyntaxException {
            List<Token> tokens = tokenizer.tokenize(expression);
            Parser parser = new Parser(tokens, source);
            return parser.parse();
        }
    }

    // ==================== Tokenizer ====================

    private static class Tokenizer {
        private static final Pattern TOKEN_PATTERN = Pattern.compile(
                "\\d+(?:\\.\\d+)?|" +                  // Numbers (with optional decimals)
                        "@[aeprs](?:\\[[^]]*])?|" +        // Selectors with optional brackets
                        "[a-zA-Z_#][a-zA-Z0-9_]*|" +           // Identifiers/functions/names (including # for fake players)
                        "[+\\-*/%^()]|" +                      // Operators and parens
                        "\\?|" +                               // Scoreboard separator
                        ","                                    // Comma for function args
        );

        public List<Token> tokenize(String input) {
            List<Token> tokens = new ArrayList<>();
            Matcher matcher = TOKEN_PATTERN.matcher(input);

            while (matcher.find()) {
                String value = matcher.group();
                if (!value.trim().isEmpty()) {
                    tokens.add(new Token(getTokenType(value), value));
                }
            }

            return tokens;
        }

        private TokenType getTokenType(String value) {
            if (value.matches("\\d+(?:\\.\\d+)?")) return TokenType.NUMBER;
            if (value.matches("[+\\-*/%^]")) return TokenType.OPERATOR;
            switch (value) {
                case "(" -> {
                    return TokenType.LPAREN;
                }
                case ")" -> {
                    return TokenType.RPAREN;
                }
                case "?" -> {
                    return TokenType.QUESTION;
                }
                case "," -> {
                    return TokenType.COMMA;
                }
            }
            if (value.startsWith("@")) return TokenType.SELECTOR;
            return TokenType.IDENTIFIER;
        }
    }

    private enum TokenType {
        NUMBER, OPERATOR, LPAREN, RPAREN, IDENTIFIER, SELECTOR, QUESTION, COMMA
    }

    private static class Token {
        final TokenType type;
        final String value;

        Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return type + ":" + value;
        }
    }

    // ==================== Parser ====================

    private static class Parser {
        private final List<Token> tokens;
        private final ServerCommandSource source;
        private int position = 0;

        public Parser(List<Token> tokens, ServerCommandSource source) {
            this.tokens = tokens;
            this.source = source;
        }

        public double parse() throws CommandSyntaxException {
            double result = parseExpression();
            if (position < tokens.size()) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
            }
            return result;
        }

        private double parseExpression() throws CommandSyntaxException {
            return parseAddSub();
        }

        private double parseAddSub() throws CommandSyntaxException {
            double left = parseMulDiv();

            while (position < tokens.size()) {
                Token token = tokens.get(position);
                if (token.type != TokenType.OPERATOR) break;
                if (!token.value.equals("+") && !token.value.equals("-")) break;

                position++;
                double right = parseMulDiv();

                if (token.value.equals("+")) {
                    left = left + right;
                } else {
                    left = left - right;
                }
            }

            return left;
        }

        private double parseMulDiv() throws CommandSyntaxException {
            double left = parsePower();

            while (position < tokens.size()) {
                Token token = tokens.get(position);
                if (token.type != TokenType.OPERATOR) break;
                if (!token.value.equals("*") && !token.value.equals("/") && !token.value.equals("%")) break;

                position++;
                double right = parsePower();

                switch (token.value) {
                    case "*" -> left = left * right;
                    case "/" -> {
                        if (right == 0) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                        left = left / right;
                    }
                    case "%" -> {
                        if (right == 0) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                        left = left % right;
                    }
                }
            }

            return left;
        }

        private double parsePower() throws CommandSyntaxException {
            double left = parseUnary();

            if (position < tokens.size()) {
                Token token = tokens.get(position);
                if (token.type == TokenType.OPERATOR && token.value.equals("^")) {
                    position++;
                    double right = parsePower(); // Right associative
                    return Math.pow(left, right);
                }
            }

            return left;
        }

        private double parseUnary() throws CommandSyntaxException {
            if (position < tokens.size()) {
                Token token = tokens.get(position);
                if (token.type == TokenType.OPERATOR && (token.value.equals("+") || token.value.equals("-"))) {
                    position++;
                    double value = parseUnary();
                    return token.value.equals("-") ? -value : value;
                }
            }

            return parsePrimary();
        }

        private double parsePrimary() throws CommandSyntaxException {
            if (position >= tokens.size()) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedDouble().create();
            }

            Token token = tokens.get(position);

            // Parentheses
            if (token.type == TokenType.LPAREN) {
                position++;
                double value = parseExpression();
                if (position >= tokens.size() || tokens.get(position).type != TokenType.RPAREN) {
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
                }
                position++;
                return value;
            }

            // Numbers
            if (token.type == TokenType.NUMBER) {
                position++;
                return Double.parseDouble(token.value);
            }

            // Selectors - must be followed by ?objective
            if (token.type == TokenType.SELECTOR) {
                if (position + 1 < tokens.size() && tokens.get(position + 1).type == TokenType.QUESTION) {
                    return parseScoreboardRef(token.value);
                }
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
            }

            // Identifiers - can be: pi constant, function call, or scoreboard reference
            if (token.type == TokenType.IDENTIFIER) {
                String name = token.value;
                String nameLower = name.toLowerCase();

                // Check for pi constant
                if (nameLower.equals("pi")) {
                    position++;
                    return Math.PI;
                }

                // Check if it's a function call
                if (position + 1 < tokens.size() && tokens.get(position + 1).type == TokenType.LPAREN) {
                    position += 2; // Skip function name and (
                    List<Double> args = parseFunctionArgs();
                    return evaluateFunction(nameLower, args);
                }

                // Check if it's a scoreboard reference (name?objective)
                if (position + 1 < tokens.size() && tokens.get(position + 1).type == TokenType.QUESTION) {
                    return parseScoreboardRef(name);
                }

                // If none of the above, it's an error
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
            }

            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedDouble().create();
        }

        private List<Double> parseFunctionArgs() throws CommandSyntaxException {
            List<Double> args = new ArrayList<>();

            // Check for empty args
            if (position < tokens.size() && tokens.get(position).type == TokenType.RPAREN) {
                position++;
                return args;
            }

            // Parse first arg (can be list from selector)
            args.addAll(parseArgument());

            // Parse remaining args
            while (position < tokens.size() && tokens.get(position).type == TokenType.COMMA) {
                position++; // Skip comma
                args.addAll(parseArgument());
            }

            // Expect closing paren
            if (position >= tokens.size() || tokens.get(position).type != TokenType.RPAREN) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
            }
            position++;

            return args;
        }

        private List<Double> parseArgument() throws CommandSyntaxException {
            // Check if this is a scoreboard reference that should expand to a list
            if (position < tokens.size()) {
                Token token = tokens.get(position);
                if ((token.type == TokenType.IDENTIFIER || token.type == TokenType.SELECTOR) &&
                        position + 1 < tokens.size() &&
                        tokens.get(position + 1).type == TokenType.QUESTION) {
                    return parseScoreboardRefList(token.value);
                }
            }

            // Otherwise parse as regular expression (returns single value)
            return List.of(parseExpression());
        }

        private double parseScoreboardRef(String selector) throws CommandSyntaxException {
            // Must consume the identifier/selector token first
            position++;

            // Expect ? followed by objective name
            if (position >= tokens.size() || tokens.get(position).type != TokenType.QUESTION) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
            }
            position++;

            if (position >= tokens.size() || tokens.get(position).type != TokenType.IDENTIFIER) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
            }

            String objective = tokens.get(position).value;
            position++;

            // Get scores - if selector returns multiple entities, average them
            List<Double> scores = getScoreboardValues(selector, objective);
            if (scores.isEmpty()) {
                return 0.0;
            }
            return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        private List<Double> parseScoreboardRefList(String selector) throws CommandSyntaxException {
            // Must consume the identifier/selector token first
            position++;

            // Expect ? followed by objective name
            if (position >= tokens.size() || tokens.get(position).type != TokenType.QUESTION) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
            }
            position++;

            if (position >= tokens.size() || tokens.get(position).type != TokenType.IDENTIFIER) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
            }

            String objective = tokens.get(position).value;
            position++;

            return getScoreboardValues(selector, objective);
        }

        private List<Double> getScoreboardValues(String selector, String objectiveName) throws CommandSyntaxException {
            Scoreboard scoreboard = source.getServer().getScoreboard();
            ScoreboardObjective objective = scoreboard.getNullableObjective(objectiveName);

            if (objective == null) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
            }

            List<String> holders = resolveSelector(selector);
            List<Double> values = new ArrayList<>();

            for (String holder : holders) {
                ScoreHolder scoreHolder = ScoreHolder.fromName(holder);
                ScoreAccess score = scoreboard.getOrCreateScore(scoreHolder, objective);
                values.add((double) score.getScore());
            }

            return values;
        }

        private List<String> resolveSelector(String selector) throws CommandSyntaxException {
            // If it's not a selector, treat it as a direct name (fake player or real player name)
            if (!selector.startsWith("@")) {
                return List.of(selector);
            }

            // Parse the selector using Minecraft's entity argument parser
            try {
                Collection<? extends net.minecraft.entity.Entity> entities =
                        EntityArgumentType.entities().parse(new com.mojang.brigadier.StringReader(selector))
                                .getEntities(source);

                List<String> names = new ArrayList<>();
                for (net.minecraft.entity.Entity entity : entities) {
                    names.add(entity.getNameForScoreboard());
                }
                return names;
            } catch (CommandSyntaxException e) {
                throw e;
            }
        }

        private double evaluateFunction(String name, List<Double> args) throws CommandSyntaxException {
            return switch (name) {
                case "min" -> {
                    if (args.isEmpty()) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                    yield args.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                }
                case "max" -> {
                    if (args.isEmpty()) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                    yield args.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                }
                case "sum" -> args.stream().mapToDouble(Double::doubleValue).sum();
                case "avg" -> {
                    if (args.isEmpty()) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                    yield args.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                }
                case "len" -> (double) args.size();
                case "abs" -> {
                    if (args.size() != 1) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                    yield Math.abs(args.getFirst());
                }
                case "floor" -> {
                    if (args.size() != 1) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                    yield Math.floor(args.getFirst());
                }
                case "ceil" -> {
                    if (args.size() != 1) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                    yield Math.ceil(args.getFirst());
                }
                case "round" -> {
                    if (args.size() != 1) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                    yield (double) Math.round(args.getFirst());
                }
                case "sqrt" -> {
                    if (args.size() != 1) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                    yield Math.sqrt(args.getFirst());
                }
                case "pow" -> {
                    if (args.size() != 2) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                    yield Math.pow(args.get(0), args.get(1));
                }
                case "sin" -> {
                    if (args.size() != 1) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                    yield Math.sin(args.getFirst());
                }
                case "cos" -> {
                    if (args.size() != 1) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                    yield Math.cos(args.getFirst());
                }
                case "tan" -> {
                    if (args.size() != 1) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                    yield Math.tan(args.getFirst());
                }
                case "clamp" -> {
                    if (args.size() != 3) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
                    yield Math.max(args.get(1), Math.min(args.get(2), args.get(0)));
                }
                default -> throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
            };
        }
    }
}