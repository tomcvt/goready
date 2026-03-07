import Spike from './spike.js';

export default class Wall {
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
