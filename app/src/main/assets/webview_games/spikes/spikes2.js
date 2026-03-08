import GameScene from './scenes/gameScene.js';

const config = {
    type: Phaser.WEBGL,
    backgroundColor: '#3d3d4d',
    physics: {
        default: 'matter',
        matter: {
            gravity: { x: 0, y: 0.5 },
            debug: true
        }
    },
    parent: 'game-container',
    scale: {
        mode: Phaser.Scale.FIT,
        autoCenter: Phaser.Scale.CENTER_BOTH,
        width: 360,
        height: 640
    },
    fps: {
        target: 60,
        forceSetTimeOut: true
    },
    scene: GameScene
}

var game = null

function bootWhenReady() {
    var el = document.getElementById('game-container')
    if (el && el.offsetHeight > 0) {
        console.log('BOOT: container ready ' + el.offsetWidth + 'x' + el.offsetHeight)
        game = new Phaser.Game(config)
    } else {
        console.log('BOOT: waiting for container...')
        requestAnimationFrame(bootWhenReady)
    }
}

if (document.readyState === 'complete') {
    bootWhenReady()
} else {
    window.addEventListener('load', bootWhenReady)
}

document.addEventListener('keydown', (e) => {
    if (e.key === 'p') {
        if (game.scene.isPaused('GameScene')) {
            game.scene.resume('GameScene')
        } else {
            game.scene.pause('GameScene')
        }
    }
})