export default class Bird extends Phaser.Physics.Matter.Sprite {
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