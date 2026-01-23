package app.simplecloud.plugin.sign.shared.config.layout

/**
 * Defines the type of servers a layout applies to.
 */
enum class LayoutType {
    /**
     * Layout applies to all server types (groups and persistent servers).
     */
    ALL,

    /**
     * Layout applies only to group-based servers.
     */
    GROUP,

    /**
     * Layout applies only to persistent servers.
     */
    PERSISTENT
}
