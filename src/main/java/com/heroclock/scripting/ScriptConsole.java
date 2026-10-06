package com.heroclock.scripting;

import com.heroclock.HeroClock;

public final class ScriptConsole {
    private final String source;
    public ScriptConsole(String source) { this.source = source; }
    public void info(String message) { HeroClock.LOGGER.info("[{}] {}", source, message); }
    public void warn(String message) { HeroClock.LOGGER.warn("[{}] {}", source, message); }
    public void error(String message) { HeroClock.LOGGER.error("[{}] {}", source, message); }
    public void log(String message) { info(message); }
}
