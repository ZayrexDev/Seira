package xyz.zcraft.data;

public record ShortcutTarget(
        Long explicitId,
        Integer boundUid,
        String macroType,
        Long macroIndex,
        String errorMessage
) {
    public boolean isMacro() { return macroType != null; }
    public boolean isError() { return errorMessage != null; }
}