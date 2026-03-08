// spikes2.bundle.js - auto-generated, do not edit
(function() {

// === AndroidBridge.js ===
// AndroidBridge.js
const presentInAndroid = typeof window !== 'undefined' && window.AndroidBridge? true : false

const Bridge = {
    onGameFinished: (score) => {
        try {
            window.AndroidBridge.onGameFinished(score)
        } catch (e) {
            console.log('AndroidBridge not available', e)
        }
    },
    onInteraction: () => {
        try {
            window.AndroidBridge.onInteraction()
        } catch (e) {
            console.log('AndroidBridge not available', e)
        }
    },
    isSkippable: () => {
        try {
            return window.AndroidBridge.isSkippable()
        } catch (e) {
            console.log('AndroidBridge not available', e)
            return false  // sensible default for browser testing
        }
    }
}

Bridge

// === objects/spike.js ===
class Spike extends Phaser.GameObjects.GameObject {
    /**
     * @param {Phaser.Scene} scene - the scene to which this spike belongs
     * @param {number} x - the x coordinate of the spike tip
     * @param {number} y - the y coordinate of the spike tip
     * @param {string} side - 'left' or 'right', which side the spike is on (for orientation)
     */
    constructor(scene, x, y, side, debug = false) {
        super(scene, 'Spike')
        this.scene = scene
        const tipX = side === 'left' ? 20 : -20
        const HEIGHT = 20
        const WIDTH = 45
        this.tweenOffset = -tipX

        // visual
        /*
        this.graphic = scene.add.triangle(
            x, y,
            tipX, 0,    // tip
            0, -22.5,   // top base
            0, 22.5,    // bottom base
            0xffffff
        )
        */
        // for some reason it works but there is an offset, so using graphics instead of shape

        /*
        this.graphic = scene.add.graphics({ x: x, y: y })
        this.graphic.fillStyle(0xffffff)
        this.graphic.beginPath()
        this.graphic.moveTo(tipX, 0)
        this.graphic.lineTo(0, -22.5)
        this.graphic.lineTo(0, 22.5)
        this.graphic.closePath()
        this.graphic.fillPath()
*/
        this.graphic = scene.add.graphics()
        this.graphic.fillStyle(0xffffff)
        this.graphic.fillTriangle(
            tipX, 0,     // tip
            0, -22.5,    // top base
            0, 22.5      // bottom base
        )
        this.graphic.setPosition(x + this.tweenOffset, y)
        // physics - added later when fully extended
        this.body = null
        this.x = x
        this.y = y
        this.debug = debug
        if (debug) {
            this.scene.add.circle(this.x, this.y, 2, 0x00ff00)  // debug body position
        }
        this.side = side
    }

    addBody() {
        const tipX = this.side === 'left' ? 20 : -20
        const offsetX = tipX / 3  // adjust body position so it better matches the graphic

        if (this.debug) {
            this.scene.add.circle(this.x, this.y, 2, 0xff0000)  // debug body position
        }

        this.body = this.scene.matter.add.fromVertices(
            this.x + offsetX, this.y,
            [
                { x: tipX - offsetX, y: 0 },
                { x: 0 - offsetX, y: -22.5 },
                { x: 0 - offsetX, y: 22.5 }
            ],
            { isStatic: true, label: 'spike', }
        )
    }

    removeBody() {
        if (this.body) {
            this.scene.matter.world.remove(this.body)
            this.body = null
        }
    }

    destroy() {
        this.removeBody()
        this.graphic.destroy()
    }
}

// === objects/bird.js ===
class Bird extends Phaser.Physics.Matter.Sprite {
    constructor(scene) {
        const gfx = scene.make.graphics({ x: 0, y: 0, add: false })
        gfx.fillStyle(0xffff00)
        gfx.fillCircle(20, 20, 20)
        gfx.generateTexture('bird', 40, 40)
        gfx.destroy()

        super(scene.matter.world, 180, 320, 'bird', undefined, {
            shape: { type: 'circle', radius: 20 },
            isStatic: false,
            frictionAir: 0,
            restitution: 1
        })
        scene.add.existing(this)

        this.isAlive = true

        this.setVelocityX(3)
    }

    flap() {
        this.setVelocityY(-4)
    }

    kill() {
        this.isAlive = false
        this.setTint(0xff0000)
        this.setVelocity(0, 0)
    }
}

// === objects/wall.js ===
class Wall {
    /**
     * @param {Phaser.Scene} scene - the scene to which this wall belongs
     * @param {string} side - 'left' or 'right', which side the wall is on (for orientation)
     * @param {boolean} debug - whether to enable debug mode (shows body positions)
     */
    constructor(scene, side, debug = false) {
        this.scene = scene
        this.side = side  // 'left' or 'right'
        this.spikes = []
        this.bodies = []

        this.debug = debug
        
        // wall x position
        this.wallX = side === 'left' ? 2.5 : 357.5
        // spike tip points inward
        this.direction = side === 'left' ? 1 : -1

        // create backing wall (rectangle with collision)
        const thickness = 5
        if (side === 'left') {
            this.backingWall = scene.matter.add.rectangle(0, 320, thickness, 640, { isStatic: true, label: `${side}Wall` })
        } else {
            this.backingWall = scene.matter.add.rectangle(360, 320, thickness, 640, { isStatic: true, label: `${side}Wall` })
        }
        if (debug) {
            scene.add.circle(this.wallX, 72.5, 2, 0x0000ff)  // debug backing wall position
            scene.add.circle(this.wallX, 567.5, 2, 0x0000ff)  // debug backing wall position
        }
        this.createSpikes()
    }

    createSpikes() {
        const count = Phaser.Math.Between(3, 6)
        const slots = [0,1,2,3,4,5,6,7,8,9,10]
        Phaser.Utils.Array.Shuffle(slots)
        const chosen = slots.slice(0, count)

        chosen.forEach(slot => {
            const y = 72.5 + (slot * 45) + 22.5
            const spike = new Spike(this.scene, this.wallX, y, this.side, this.debug)
            this.spikes.push(spike)
        })
    }

    extend() {
        // animate spikes sliding out
        let counter = this.spikes.length
        this.spikes.forEach(spike => {
            this.scene.tweens.add({
                targets: spike.graphic,
                x: this.wallX,  // slide out
                duration: 200,
                onComplete: () => {
                    counter--
                    if (counter === 0) {
                        this.addBodies()
                    }  // collision after fully extended
                }
            })
        })
    }

    addBodies() {
        this.spikes.forEach(spike => {
            spike.addBody()
            this.bodies.push(spike.body)
        })
    }

    hideAndRetract(doRetract = true) {
        // destroy collision bodies immediately
        this.bodies.forEach(body => {
            this.scene.matter.world.remove(body)
        })
        this.bodies = []
        if (!doRetract) return
        // animate spikes back into wall
        this.scene.tweens.add({
            targets: this.spikes.map(s => s.graphic),
            x: this.wallX + (this.spikes[0].tweenOffset),  // slide back in
            duration: 200,
            onComplete: () => {
                // destroy visuals
                this.spikes.forEach(s => s.destroy())
                this.spikes = []
                // wait then create new ones
                this.scene.time.delayedCall(1000, () => {
                    this.createSpikes()
                    this.extend()
                })
            }
        })
    }

    destroy() {
        this.scene.matter.world.remove(this.backingWall)
        this.hideAndRetract(false)
        this.backingWall = null
    }
}

// === scenes/gameScene.js ===
class GameScene extends Phaser.Scene {
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

// === spikes2.js ===
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

})();