package xyz.zcraft.data;

public record ShortcutTarget(
        Integer explicitId,
        Integer boundUid,
        String macroIndex,
        String errorMessage
) {
    public boolean isMacro() {
        return boundUid != null && macroIndex != null;
    }

    public boolean isError() {
        return errorMessage != null;
    }
}