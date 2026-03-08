interface AndroidBridge {
    onGameFinished(score: number): void;
    onInteraction(): void;
    isSkippable(): boolean;
}

interface Window {
    AndroidBridge?: AndroidBridge;
}
