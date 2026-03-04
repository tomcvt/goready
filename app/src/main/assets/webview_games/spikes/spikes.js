const config = {
    type: Phaser.AUTO,
    backgroundColor: '#1a1a2e',
    scale: {
        mode: Phaser.Scale.FIT,
        autoCenter: Phaser.Scale.CENTER_BOTH,
        width: 360,
        height: 640
    },
    scene: GameScene
}

const game = new Phaser.Game(config)