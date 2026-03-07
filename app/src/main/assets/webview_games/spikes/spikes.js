import GameScene from './scenes/gameScene.js';

const config = {
    type: Phaser.AUTO,
    backgroundColor: '#1a1a2e',
    physics: {
        default: 'matter',
        matter: {
            gravity: { y: 0.5 },
            debug: true
        }
    },
    scale: {
        mode: Phaser.Scale.FIT,
        autoCenter: Phaser.Scale.CENTER_BOTH,
        width: 360,
        height: 640
    },
    scene: GameScene
}

const game = new Phaser.Game(config)