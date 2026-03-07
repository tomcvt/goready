export default class Bird extends Phaser.GameObjects.Sprite {
    constructor(scene) {
        const gfx = scene.make.graphics({ x: 0, y: 0, add: false })
        gfx.fillStyle(0xffff00)
        gfx.fillCircle(20, 20, 20)
        gfx.generateTexture('bird', 40, 40)
        gfx.destroy()

        super(scene, 180, 320, 'bird')
        scene.add.existing(this)
        scene.matter.add.gameObject(this, {
            shape: { type: 'circle', radius: 20 },
            isStatic: false,
            frictionAir: 0,
            restitution: 1
        })

        this.isAlive = true

        this.setVelocityX(3)
    }

    flap() {
        this.setVelocityY(-5)
    }

    kill() {
        this.isAlive = false
        this.setTint(0xff0000)
        this.setVelocity(0, 0)
    }
}