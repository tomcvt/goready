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

export default Bridge