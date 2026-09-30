package com.example.data.model

enum class MotionType(val displayName: String) {
    NONE("Static"),
    ZOOM_IN("Zoom In"),
    ZOOM_OUT("Zoom Out"),
    PAN_LEFT("Pan Left"),
    PAN_RIGHT("Pan Right"),
    TILT_UP("Tilt Up"),
    TILT_DOWN("Tilt Down"),
    ROTATION("Rotation"),
    SPEED_RAMP("Speed Ramp"),
    WHIP_PAN("Whip Pan"),
    HOLD("Hold / Pause"),
    COMBINED("Dynamic Motion")
}

enum class EasingType(val displayName: String) {
    LINEAR("Linear"),
    EASE_IN("Ease In"),
    EASE_OUT("Ease Out"),
    EASE_IN_OUT("Ease In Out"),
    CUBIC("Cubic Bézier"),
    SMOOTH("Smooth S-Curve")
}

enum class TimingMappingMode(val title: String, val subtitle: String) {
    NORMALIZE("Normalized Scale (0-100%)", "Maps reference motion curves proportionally across target duration"),
    STRETCH("Stretch Timing", "Stretches reference keyframe intervals to match longer target"),
    COMPRESS("Compress Timing", "Compresses reference keyframes into shorter target duration"),
    SCENE_BASED("Scene Anchor Mapping", "Aligns motion peaks with detected scene changes in target")
}
