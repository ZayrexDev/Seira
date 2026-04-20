package xyz.zcraft.data;

public record ShortcutTarget(
        Long explicitId,
        Integer boundUid,
        String macroType,
        Long macroIndex,
        String errorMessage
) {
    public boolean isMacro() {
        return boundUid != null && macroIndex != null && macroType != null;
    }

    public boolean isError() {
        return errorMessage != null;
    }

    public String getOfString() {
        return macroType + macroIndex;
    }
}