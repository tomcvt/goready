class Bird extends Phaser.GameObjects.Sprite {
    constructor(scene) {
        super(scene, 180, 320, 20, 0xffff00)

        const texture = this.make.graphics()
        texture.fillStyle(0xffff00)
        texture.fillCircle(20, 20, 20)
        texture.generateTexture('bird', 40, 40)

        scene.add.existing(this)
        scene.matter.add.gameObject(this, { 
            label: 'bird',
            isStatic: false,
            shape: 'circle'
        })
        this.setVelocityX(3)
    }

    flap() {
        this.setVelocityY(-8)
    }
}