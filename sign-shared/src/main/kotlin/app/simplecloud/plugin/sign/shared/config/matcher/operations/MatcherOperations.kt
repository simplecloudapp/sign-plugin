package app.simplecloud.plugin.sign.shared.config.matcher.operations

enum class MatcherOperations(val matcher: OperationMatcher) {

    REGEX(RegexOperationMatcher()),
    EQUALS(EqualsOperationMatcher()),
    EQUALS_IGNORE_CASE(EqualsIgnoreCaseOperationMatcher()),
    NOT_EQUALS(NotEqualsOperationMatcher()),
    CONTAINS(ContainsOperationMatcher()),
    STARTS_WITH(StartsWithOperationMatcher()),
    ENDS_WITH(EndsWithOperationMatcher()),
    MATCHES_PATTERN(PatternOperationMatcher());

    fun matches(key: String, value: String): Boolean {
        return matcher.matches(key, value);
    }

}