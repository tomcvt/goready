import Bird from '../objects/bird.js';
import Wall from '../objects/wall.js';
import Bridge from '../AndroidBridge.js';

export default class GameScene extends Phaser.Scene {
    constructor() {
        super({ key: 'GameScene' })
    }

    preload() {

    }

    create() {
        console.log('SCENE: create() started');
        try {

        this.score = 0

        this.bird = new Bird(this)
        console.log('SCENE: bird created');

        const thickness = 25
        //this.matter.add.rectangle(0, 320, thickness, 640, { isStatic: true, label: 'leftWall' })
        //this.matter.add.rectangle(360, 320, thickness, 640, { isStatic: true, label: 'rightWall' })
        this.matter.add.rectangle(180, 0, 360, thickness, { isStatic: true, label: 'ceiling' })
        this.matter.add.rectangle(180, 640, 360, thickness, { isStatic: true, label: 'floor' })

        this.debug = Boolean(this.sys.game.config.physics.matter.debug)

        this.leftWall = new Wall(this, 'left', this.debug)
        this.rightWall = new Wall(this, 'right', this.debug)
        this.leftWall.extend()
        console.log('SCENE: walls created');
        this.rightWall.extend()

        this.input.on('pointerdown', () => {
            if (this.bird.isAlive) {
                this.bird.flap()
            }
        })

        this.matter.world.on('collisionstart', (event) => {
            event.pairs.forEach(pair => {
                const labels = [pair.bodyA.label, pair.bodyB.label]

                if (labels.includes('leftWall')) {
                    const vx = this.bird.body.velocity.x
                    const vy = this.bird.body.velocity.y
                    this.bird.setVelocity(-vx, vy)  // flip x, keep y
                    this.leftWall.hideAndRetract()  // hide spikes and retract
                    this.score++
                    Bridge.onInteraction()  // notify Android of interaction , skip button is onGameFinished
                }
                else if (labels.includes('rightWall')) {
                    const vx = this.bird.body.velocity.x
                    const vy = this.bird.body.velocity.y
                    this.bird.setVelocity(-vx, vy)  // flip x, keep y
                    this.rightWall.hideAndRetract()  // hide spikes and retract
                    this.score++
                    Bridge.onInteraction()  // notify Android of interaction , skip button is onGameFinished
                }
                else if (labels.includes('spike')) {
                    //turn off gravity so it doesn't fall after death
                    this.bird.setIgnoreGravity(true)
                    this.bird.setVelocity(0, 0)  // stop movement
                    this.bird.kill()  // kill immediately, no bounce
                    if (this.score > 9) {
                        Bridge.onGameFinished(this.score)  // notify Android of game finished with score
                    }
                    this.time.delayedCall(3000, () => this.restart())  // restart after delay
                }

                if (labels.includes('ceiling') || labels.includes('floor')) {
                    const vx = this.bird.body.velocity.x
                    const vy = this.bird.body.velocity.y
                    this.bird.setIgnoreGravity(true)
                    this.bird.setVelocity(0, 0)  // flip y, keep x
                    this.bird.kill()  // kill
                    if (this.score > 9) {
                        Bridge.onGameFinished(this.score)  // notify Android of game finished with score
                    }
                    this.time.delayedCall(3000, () => this.restart())  // restart after delay
                }
            })
        })
        console.log('SCENE: create() finished');
        } catch(e) { console.error('SCENE ERROR:', e.message, e.stack); }
    }

    update() {

    }

    restart() {
        this.scene.restart()
    }

}