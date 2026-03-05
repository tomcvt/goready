import Bird from './objects/bird.js'

export default class GameScene extends Phaser.Scene {
    constructor() {
        super({ key: 'GameScene' })
    }

    preload() {

    }

    create() {
        this.bird = new Bird(this)

        this.input.on('pointerdown', () => {
            this.bird.flap()
        })
    }

    update() {

    }
}