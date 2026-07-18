package my.hive.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerFacingChineseMessageContractTest {
    private static final Pattern STRING_LITERAL = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern ASCII_WORD = Pattern.compile("[A-Za-z]{3,}");
    private static final Pattern CHINESE_TEXT = Pattern.compile("[\\u4e00-\\u9fff]");
    private static final Set<String> ALLOWED_TECHNICAL_LITERALS = Set.of("MB");

    @Test
    void businessExceptionScannerSelectsOnlyTheCustomerMessageArgument() {
        assertThat(businessExceptionMessageExpression("""
                return new BusinessException(
                        403,
                        AuthReason.EMPLOYEE_NOT_FOUND,
                        "管理员尚未添加该手机号",
                        Map.of("phoneVerificationTicket", ticket)
                );
                """))
                .isEqualTo("\"管理员尚未添加该手机号\"");

        String englishMessage = businessExceptionMessageExpression(
                "throw new BusinessException(409, \"PHONE_ACCOUNT_AMBIGUOUS\", \"account unavailable\");");
        assertThat(englishMessage).isEqualTo("\"account unavailable\"");
        Matcher englishLiteral = STRING_LITERAL.matcher(englishMessage);
        assertThat(englishLiteral.find()).isTrue();
        assertThat(ASCII_WORD.matcher(englishLiteral.group(1)).find()).isTrue();
        assertThat(CHINESE_TEXT.matcher(englishLiteral.group(1)).find()).isFalse();
    }

    @Test
    void businessExceptionsDoNotExposePureEnglishCustomerMessages() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        List<String> violations = new ArrayList<>();

        try (var paths = Files.walk(sourceRoot)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> inspect(path, violations));
        }

        assertThat(violations)
                .as("BusinessException messages returned to clients must contain Chinese guidance")
                .isEmpty();
    }

    private void inspect(Path path, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                boolean businessExceptionMessage = line.contains("new BusinessException");
                boolean annotationMessage = line.trim().startsWith("@") && line.contains("message =");
                boolean directResponseMessage = line.contains("Result.fail(") || line.contains("writeErrorResponse(");
                if (!businessExceptionMessage && !annotationMessage && !directResponseMessage) {
                    continue;
                }
                String expression = businessExceptionMessage
                        ? businessExceptionMessageExpression(collectStatement(lines, index))
                        : line;
                Matcher matcher = STRING_LITERAL.matcher(expression);
                while (matcher.find()) {
                    String literal = matcher.group(1).trim();
                    if (ALLOWED_TECHNICAL_LITERALS.contains(literal)
                            || !ASCII_WORD.matcher(literal).find()
                            || CHINESE_TEXT.matcher(literal).find()) {
                        continue;
                    }
                    violations.add(path + ":" + (index + 1) + " -> " + literal);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法检查客户可见异常文案: " + path, exception);
        }
    }

    private String collectStatement(List<String> lines, int startIndex) {
        StringBuilder statement = new StringBuilder();
        for (int index = startIndex; index < lines.size(); index++) {
            String line = lines.get(index);
            statement.append(line).append('\n');
            if (line.contains(";")) {
                break;
            }
        }
        return statement.toString();
    }

    private String businessExceptionMessageExpression(String statement) {
        int constructor = statement.indexOf("new BusinessException");
        int openParenthesis = statement.indexOf('(', constructor);
        if (constructor < 0 || openParenthesis < 0) {
            return statement;
        }

        List<String> arguments = new ArrayList<>();
        int argumentStart = openParenthesis + 1;
        int nestedDepth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = argumentStart; index < statement.length(); index++) {
            char current = statement.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '(' || current == '[' || current == '{') {
                nestedDepth++;
            } else if (current == ')' && nestedDepth == 0) {
                arguments.add(statement.substring(argumentStart, index).trim());
                break;
            } else if (current == ')' || current == ']' || current == '}') {
                nestedDepth--;
            } else if (current == ',' && nestedDepth == 0) {
                arguments.add(statement.substring(argumentStart, index).trim());
                argumentStart = index + 1;
            }
        }

        if (arguments.isEmpty()) {
            return statement;
        }
        int messageIndex = arguments.size() == 1 ? 0 : arguments.size() == 2 ? 1 : 2;
        return arguments.get(messageIndex);
    }
}
