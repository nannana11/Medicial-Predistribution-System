package com.example.triageclient;

/**
 * 普通 Java 启动入口，避免 IDE 直接运行 JavaFX Application 子类时
 * 出现“缺少 JavaFX 运行时组件”的错误。
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        TriageClientApplication.main(args);
    }
}
