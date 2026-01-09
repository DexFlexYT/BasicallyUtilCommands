package org.dexflex.basicallycommandutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
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
                .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
                        .then(CommandManager.argument("expression", StringArgumentType.greedyString())
                                .executes(EvalCommand::execute)
                        )
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        float scale = FloatArgumentType.getFloat(context, "scale");
        String expression = StringArgumentType.getString(context, "expression");

        ExpressionEvaluator evaluator = new ExpressionEvaluator(context.getSource());
        double result = evaluator.evaluate(expression);
        double scaled = result * scale;

        context.getSource().sendFeedback(
                () -> Text.literal("Result: " + result + " (scaled: " + scaled + ")"),
                false
        );

        return (int) scaled;
    }

    // ==================== Expression Evaluator ====================

    private static class ExpressionEvaluator {
        private final ServerCommandSource source;
        private final Tokenizer tokenizer = new Tokenizer();

        public ExpressionEvaluator(ServerCommandSource source) {
            this.source = source;
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
                "\\d+(?:\\.\\d+)?|" +
                        "@[aeprs](?:\\[[^]]*])?|" +
                        "[a-zA-Z_#][a-zA-Z0-9_]*|" +
                        "[+\\-*/%^()]|" +
                        "\\?|,"
        );

        public List<Token> tokenize(String input) {
            List<Token> tokens = new ArrayList<>();
            Matcher matcher = TOKEN_PATTERN.matcher(input);

            while (matcher.find()) {
                String v = matcher.group();
                tokens.add(new Token(typeOf(v), v));
            }
            return tokens;
        }

        private TokenType typeOf(String v) {
            if (v.matches("\\d+(?:\\.\\d+)?")) return TokenType.NUMBER;
            switch (v) {
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
            if (v.matches("[+\\-*/%^]")) return TokenType.OPERATOR;
            if (v.startsWith("@")) return TokenType.SELECTOR;
            return TokenType.IDENTIFIER;
        }
    }

    private enum TokenType {
        NUMBER, OPERATOR, LPAREN, RPAREN, IDENTIFIER, SELECTOR, QUESTION, COMMA
    }

    private record Token(TokenType type, String value) {}

    // ==================== Parser ====================

    private static class Parser {
        private final List<Token> tokens;
        private final ServerCommandSource source;
        private int pos = 0;

        Parser(List<Token> tokens, ServerCommandSource source) {
            this.tokens = tokens;
            this.source = source;
        }

        double parse() throws CommandSyntaxException {
            double v = expression();
            if (pos != tokens.size()) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
            }
            return v;
        }

        // expression -> addSub
        private double expression() throws CommandSyntaxException {
            return addSub();
        }

        // addSub -> mulDiv ((+|-) mulDiv)*
        private double addSub() throws CommandSyntaxException {
            double v = mulDiv();
            while (pos < tokens.size()) {
                Token t = tokens.get(pos);
                if (t.type != TokenType.OPERATOR || !(t.value.equals("+") || t.value.equals("-"))) break;
                pos++;
                double r = mulDiv();
                v = t.value.equals("+") ? v + r : v - r;
            }
            return v;
        }

        // mulDiv -> power ((*|/|%) power)*
        private double mulDiv() throws CommandSyntaxException {
            double v = power();
            while (pos < tokens.size()) {
                Token t = tokens.get(pos);
                if (t.type != TokenType.OPERATOR || !(t.value.equals("*") || t.value.equals("/") || t.value.equals("%"))) break;
                pos++;
                double r = power();
                switch (t.value) {
                    case "*" -> v *= r;
                    case "/" -> v /= r;
                    case "%" -> v %= r;
                }
            }
            return v;
        }

        // power -> unary (^ power)?  (right associative)
        private double power() throws CommandSyntaxException {
            double v = unary();
            if (pos < tokens.size()) {
                Token t = tokens.get(pos);
                if (t.type == TokenType.OPERATOR && t.value.equals("^")) {
                    pos++;
                    v = Math.pow(v, power());
                }
            }
            return v;
        }

        // unary -> (+|-) unary | primary
        private double unary() throws CommandSyntaxException {
            if (pos < tokens.size()) {
                Token t = tokens.get(pos);
                if (t.type == TokenType.OPERATOR && (t.value.equals("+") || t.value.equals("-"))) {
                    pos++;
                    double v = unary();
                    return t.value.equals("-") ? -v : v;
                }
            }
            return primary();
        }

        // primary
        private double primary() throws CommandSyntaxException {
            if (pos >= tokens.size()) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedDouble().create();
            Token t = tokens.get(pos);

            if (t.type == TokenType.LPAREN) {
                pos++;
                double v = expression();
                expect(TokenType.RPAREN);
                return v;
            }

            if (t.type == TokenType.NUMBER) {
                pos++;
                return Double.parseDouble(t.value);
            }

            if (t.type == TokenType.IDENTIFIER) {
                String name = t.value.toLowerCase();

                // constant
                if (name.equals("pi")) {
                    pos++;
                    return Math.PI;
                }

                // function
                if (peek(TokenType.LPAREN, 1)) {
                    pos += 2; // name + (
                    List<Double> args = functionArgs();
                    return evalFunction(name, args);
                }

                // scoreboard ref
                if (peek(TokenType.QUESTION, 1)) {
                    return scoreRef(t.value);
                }
            }

            if (t.type == TokenType.SELECTOR) {
                if (peek(TokenType.QUESTION, 1)) {
                    return scoreRef(t.value);
                }
            }

            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
        }

        // ---------- functions ----------

        private List<Double> functionArgs() throws CommandSyntaxException {
            List<Double> out = new ArrayList<>();

            if (peek(TokenType.RPAREN, 0)) {
                pos++;
                return out;
            }

            out.add(expression());
            while (peek(TokenType.COMMA, 0)) {
                pos++;
                out.add(expression());
            }

            expect(TokenType.RPAREN);
            return out;
        }

        // ---------- scoreboard ----------

        private double scoreRef(String target) throws CommandSyntaxException {
            pos++; // name/selector
            expect(TokenType.QUESTION);
            Token obj = expect(TokenType.IDENTIFIER);

            List<Double> vals = getScores(target, obj.value);
            if (vals.isEmpty()) return 0.0;
            return vals.stream().mapToDouble(d -> d).average().orElse(0.0);
        }

        private List<Double> getScores(String target, String objective) throws CommandSyntaxException {
            Scoreboard board = source.getServer().getScoreboard();
            ScoreboardObjective obj = board.getNullableObjective(objective);
            if (obj == null) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();

            List<String> holders = resolveTargets(target);
            List<Double> out = new ArrayList<>();

            for (String h : holders) {
                ScoreHolder holder = ScoreHolder.fromName(h);
                ScoreAccess s = board.getOrCreateScore(holder, obj);
                out.add((double) s.getScore());
            }
            return out;
        }

        private List<String> resolveTargets(String target) throws CommandSyntaxException {
            if (!target.startsWith("@")) return List.of(target);

            Collection<? extends net.minecraft.entity.Entity> entities =
                    EntityArgumentType.entities().parse(new StringReader(target)).getEntities(source);

            List<String> out = new ArrayList<>();
            for (var e : entities) out.add(e.getNameForScoreboard());
            return out;
        }

        // ---------- helpers ----------

        private boolean peek(TokenType t, int off) {
            int i = pos + off;
            return i < tokens.size() && tokens.get(i).type == t;
        }

        private Token expect(TokenType t) throws CommandSyntaxException {
            if (pos >= tokens.size() || tokens.get(pos).type != t)
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
            return tokens.get(pos++);
        }

        // ---------- functions impl ----------

        private double evalFunction(String name, List<Double> a) throws CommandSyntaxException {
            return switch (name) {
                case "min" -> a.stream().mapToDouble(d -> d).min().orElseThrow();
                case "max" -> a.stream().mapToDouble(d -> d).max().orElseThrow();
                case "sum" -> a.stream().mapToDouble(d -> d).sum();
                case "avg" -> a.stream().mapToDouble(d -> d).average().orElse(0);
                case "len" -> a.size();
                case "abs" -> Math.abs(one(a));
                case "floor" -> Math.floor(one(a));
                case "ceil" -> Math.ceil(one(a));
                case "round" -> Math.round(one(a));
                case "sqrt" -> Math.sqrt(one(a));
                case "pow" -> Math.pow(a.get(0), a.get(1));
                case "sin" -> Math.sin(one(a));
                case "cos" -> Math.cos(one(a));
                case "tan" -> Math.tan(one(a));
                case "clamp" -> Math.max(a.get(1), Math.min(a.get(2), a.get(0)));
                default -> throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
            };
        }

        private double one(List<Double> a) throws CommandSyntaxException {
            if (a.size() != 1) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
            return a.getFirst();
        }
    }
}
