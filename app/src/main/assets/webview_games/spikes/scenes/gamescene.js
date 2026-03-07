import Bird from '../objects/bird.js';

export default class GameScene extends Phaser.Scene {
    constructor() {
        super({ key: 'GameScene' })
    }

    preload() {

    }

    create() {
        this.bird = new Bird(this)

        const thickness = 25
        this.matter.add.rectangle(0, 320, thickness, 640, { isStatic: true, label: 'leftWall' })
        this.matter.add.rectangle(360, 320, thickness, 640, { isStatic: true, label: 'rightWall' })
        this.matter.add.rectangle(180, 0, 360, thickness, { isStatic: true, label: 'ceiling' })
        this.matter.add.rectangle(180, 640, 360, thickness, { isStatic: true, label: 'floor' })

        this.input.on('pointerdown', () => {
            if (this.bird.isAlive) {
                this.bird.flap()
            }
        })

        this.matter.world.on('collisionstart', (event) => {
            event.pairs.forEach(pair => {
                const labels = [pair.bodyA.label, pair.bodyB.label]

                if (labels.includes('leftWall') || labels.includes('rightWall')) {
                    const vx = this.bird.body.velocity.x
                    const vy = this.bird.body.velocity.y
                    this.bird.setVelocity(-vx, vy)  // flip x, keep y
                }

                if (labels.includes('ceiling') || labels.includes('floor')) {
                    const vx = this.bird.body.velocity.x
                    const vy = this.bird.body.velocity.y
                    this.bird.kill()  // kill
                }
            })
        })
    }

    update() {

    }




}