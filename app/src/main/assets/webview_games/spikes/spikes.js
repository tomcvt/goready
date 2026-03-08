import GameScene from './scenes/gameScene.js';

const config = {
    type: Phaser.CANVAS,
    backgroundColor: '#00ff00',
    physics: {
        default: 'matter',
        matter: {
            gravity: { x: 0, y: 0.5 },
            debug: true
        }
    },
    width: 360,
    height: 640,
    scene: GameScene
}

const game = new Phaser.Game(config)

document.addEventListener('keydown', (e) => {
    if (e.key === 'p') {
        if (game.scene.isPaused('GameScene')) {
            game.scene.resume('GameScene')
        } else {
            game.scene.pause('GameScene')
        }
    }
})