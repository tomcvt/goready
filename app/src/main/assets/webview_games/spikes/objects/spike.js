
export default class Spike extends Phaser.GameObjects.GameObject {
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