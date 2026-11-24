# Logs, CSV cells, and in-progress entries store Enum.name() and read it back
# with valueOf(). R8 must leave those constant names unchanged.
-keepclassmembers enum com.squishey.stoolsentry.** {
    <fields>;
    **[] $VALUES;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
