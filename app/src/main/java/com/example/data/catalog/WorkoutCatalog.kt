package com.example.data.catalog

import com.example.data.model.ExerciseDefinition
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutExerciseEntry
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutProgram

object WorkoutCatalog {

    val muscleGroups: List<String> = listOf(
        "All", "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Forearms",
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Abs", "Full Body"
    )

    val equipmentTypes: List<String> = listOf(
        "All", "Barbell", "Dumbbells", "Machines", "Cables",
        "Bodyweight", "Resistance bands", "Kettlebells"
    )

    val difficultyLevels: List<String> = listOf(
        "All", "Beginner", "Intermediate", "Advanced"
    )

    val exercises: List<ExerciseDefinition> = listOf(
        // CHEST
        ExerciseDefinition(
            id = "ex_bench_press",
            name = "Barbell Bench Press",
            muscleGroup = "Chest",
            equipment = "Barbell",
            level = "intermediate",
            metValue = 5.5,
            instructions = "Retract shoulder blades onto the bench, lower the bar smoothly to mid-chest, and drive forcefully back to lockout.",
            defaultSets = 4,
            defaultReps = 8,
            secondaryMuscles = listOf("Triceps", "Shoulders"),
            steps = listOf(
                "Plant feet flat, arch upper back subtly, and pinch shoulder blades together.",
                "Grip the bar slightly wider than shoulder width with stacked wrists.",
                "Unrack with straight arms, inhale, and lower under control to the sternum line.",
                "Touch lightly without bouncing and press up and slightly back."
            ),
            techniqueGuidance = "Keep elbows at ~45–60 degrees from the torso to protect shoulder capsules and maximize pec tension.",
            commonMistakes = listOf("Flaring elbows to 90 degrees", "Bouncing bar off sternum", "Lifting hips off the bench"),
            alternativeIds = listOf("ex_incline_db_press", "ex_chest_press_machine", "ex_pushups"),
            defaultRepMin = 6,
            defaultRepMax = 10,
            defaultRestSeconds = 120,
            illustrationPose = "press"
        ),
        ExerciseDefinition(
            id = "ex_incline_db_press",
            name = "Incline Dumbbell Press",
            muscleGroup = "Chest",
            equipment = "Dumbbells",
            level = "intermediate",
            metValue = 5.0,
            instructions = "Set bench to 30 degrees, lower dumbbells toward upper chest, and press upward in a slight inward arc.",
            defaultSets = 3,
            defaultReps = 10,
            secondaryMuscles = listOf("Shoulders", "Triceps"),
            steps = listOf(
                "Set bench incline to 30 degrees rather than 45 to keep focus on clavicular pecs.",
                "Kick weights to shoulder level and pack shoulders down and back.",
                "Lower dumbbells for 2–3 seconds until elbows dip slightly below bench height.",
                "Press smoothly without clanking weights together."
            ),
            techniqueGuidance = "Maintain forearm verticality directly beneath the dumbbells at the bottom position.",
            commonMistakes = listOf("Bench angle too steep (turns into shoulder press)", "Shrugging traps at the top", "Shortening range of motion"),
            alternativeIds = listOf("ex_bench_press", "ex_cable_fly", "ex_pushups"),
            defaultRepMin = 8,
            defaultRepMax = 12,
            defaultRestSeconds = 90,
            illustrationPose = "press"
        ),
        ExerciseDefinition(
            id = "ex_chest_press_machine",
            name = "Seated Machine Chest Press",
            muscleGroup = "Chest",
            equipment = "Machines",
            level = "beginner",
            metValue = 4.2,
            instructions = "Adjust seat so handles align with mid-chest, keep shoulders pinned back, and press handles forward.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Triceps", "Shoulders"),
            steps = listOf(
                "Align seat height so handles sit at mid-sternum level.",
                "Press lower back and shoulder blades firmly into the pad.",
                "Extend arms cleanly without locking elbows forcefully, then return slowly."
            ),
            techniqueGuidance = "Ideal for safely training close to technical failure without a spotter.",
            commonMistakes = listOf("Rounding shoulders forward at lockout", "Seat set too high causing neck strain"),
            alternativeIds = listOf("ex_bench_press", "ex_incline_db_press", "ex_pushups"),
            defaultRepMin = 10,
            defaultRepMax = 15,
            defaultRestSeconds = 75,
            illustrationPose = "press"
        ),
        ExerciseDefinition(
            id = "ex_cable_fly",
            name = "Cable Chest Fly",
            muscleGroup = "Chest",
            equipment = "Cables",
            level = "beginner",
            metValue = 3.5,
            instructions = "Step into a staggered stance, keep soft elbows, and sweep handles forward in a wide hug arc.",
            defaultSets = 3,
            defaultReps = 14,
            secondaryMuscles = listOf("Shoulders"),
            steps = listOf(
                "Set pulleys to chest height and step forward for steady tension.",
                "Keep a gentle 15-degree bend locked into your elbows.",
                "Bring hands together at lower chest level and squeeze for one second."
            ),
            techniqueGuidance = "Focus on bringing biceps toward each other rather than pushing hands straight forward.",
            commonMistakes = listOf("Turning the fly into a press", "Over-stretching shoulders past torso plane"),
            alternativeIds = listOf("ex_band_chest_fly", "ex_incline_db_press"),
            defaultRepMin = 12,
            defaultRepMax = 15,
            defaultRestSeconds = 60,
            illustrationPose = "fly"
        ),
        ExerciseDefinition(
            id = "ex_pushups",
            name = "Push-Ups",
            muscleGroup = "Chest",
            equipment = "Bodyweight",
            level = "beginner",
            metValue = 4.0,
            instructions = "Maintain a rigid plank from heels to crown, lower chest toward floor with elbows angled 45 degrees, and press back up.",
            defaultSets = 3,
            defaultReps = 15,
            secondaryMuscles = listOf("Triceps", "Shoulders", "Abs"),
            steps = listOf(
                "Place hands slightly wider than shoulders and screw palms outward into the floor.",
                "Brace abs and squeeze glutes before bending elbows.",
                "Lower until chest hovers above floor, then push the ground away."
            ),
            techniqueGuidance = "Elevate hands on a bench if needed to preserve strict core alignment and full depth.",
            commonMistakes = listOf("Sagging hips", "Flaring elbows perpendicular to torso", "Half-reps"),
            alternativeIds = listOf("ex_incline_db_press", "ex_band_chest_press", "ex_chest_press_machine"),
            defaultRepMin = 10,
            defaultRepMax = 20,
            defaultRestSeconds = 60,
            illustrationPose = "press"
        ),
        ExerciseDefinition(
            id = "ex_band_chest_press",
            name = "Resistance Band Chest Press",
            muscleGroup = "Chest",
            equipment = "Resistance bands",
            level = "beginner",
            metValue = 3.5,
            instructions = "Anchor band behind upper back or doorway at chest height, stagger stance, and press forward smoothly.",
            defaultSets = 3,
            defaultReps = 15,
            secondaryMuscles = listOf("Triceps", "Shoulders"),
            steps = listOf(
                "Wrap band safely across shoulder blades or anchor behind you.",
                "Press handles forward until arms extend, feeling peak resistance at lockout.",
                "Control the return back to chest level for 2 seconds."
            ),
            techniqueGuidance = "Bands peak at lockout—pause briefly at full extension for maximum chest contraction.",
            commonMistakes = listOf("Band sliding up onto neck", "Snapping arms back passively"),
            alternativeIds = listOf("ex_pushups", "ex_cable_fly"),
            defaultRepMin = 12,
            defaultRepMax = 18,
            defaultRestSeconds = 60,
            illustrationPose = "press"
        ),

        // BACK
        ExerciseDefinition(
            id = "ex_barbell_row",
            name = "Bent-Over Barbell Row",
            muscleGroup = "Back",
            equipment = "Barbell",
            level = "intermediate",
            metValue = 6.0,
            instructions = "Hinge at hips to a 35–45 degree torso angle with a braced spine, and pull the barbell toward your lower ribcage.",
            defaultSets = 4,
            defaultReps = 8,
            secondaryMuscles = listOf("Biceps", "Forearms", "Shoulders"),
            steps = listOf(
                "Stand hip-width, hinge hips back with soft knees and flat back.",
                "Pull bar toward belly button/lower sternum by driving elbows behind hips.",
                "Pause briefly at torso contact and lower with control."
            ),
            techniqueGuidance = "Keep neck neutral and avoid standing upright using lower-back momentum.",
            commonMistakes = listOf("Rounding lumbar spine", "Jerking torso upward to finish reps"),
            alternativeIds = listOf("ex_db_row", "ex_seated_cable_row", "ex_band_row"),
            defaultRepMin = 6,
            defaultRepMax = 10,
            defaultRestSeconds = 120,
            illustrationPose = "row"
        ),
        ExerciseDefinition(
            id = "ex_pullups",
            name = "Pull-Ups",
            muscleGroup = "Back",
            equipment = "Bodyweight",
            level = "advanced",
            metValue = 8.0,
            instructions = "Grip bar slightly outside shoulder width, depress shoulder blades, and pull chest toward the bar.",
            defaultSets = 4,
            defaultReps = 8,
            secondaryMuscles = listOf("Biceps", "Forearms", "Abs"),
            steps = listOf(
                "Start from a dead hang with ribs stacked over pelvis.",
                "Initiate by pulling shoulder blades down before bending elbows.",
                "Drive elbows down to sides until chin clears the bar cleanly."
            ),
            techniqueGuidance = "Lead with your chest rather than craning your chin forward at the top.",
            commonMistakes = listOf("Kipping or swinging legs", "Stopping short of full arm extension at bottom"),
            alternativeIds = listOf("ex_lat_pulldown", "ex_band_row"),
            defaultRepMin = 5,
            defaultRepMax = 10,
            defaultRestSeconds = 120,
            illustrationPose = "pull"
        ),
        ExerciseDefinition(
            id = "ex_lat_pulldown",
            name = "Lat Pulldown",
            muscleGroup = "Back",
            equipment = "Cables",
            level = "beginner",
            metValue = 4.2,
            instructions = "Grip wider than shoulders, lean back slightly, and pull the bar smoothly toward your upper collarbone.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Biceps", "Forearms"),
            steps = listOf(
                "Lock thighs firmly under knee pads and grip bar outside shoulder width.",
                "Depress shoulder blades first, then drive elbows down toward ribs.",
                "Control the bar back overhead to a full lat stretch."
            ),
            techniqueGuidance = "Think of your hands as hooks and pull through your elbows to keep biceps from dominating.",
            commonMistakes = listOf("Swinging backward aggressively", "Pulling bar behind the neck"),
            alternativeIds = listOf("ex_pullups", "ex_band_pulldown", "ex_seated_cable_row"),
            defaultRepMin = 8,
            defaultRepMax = 12,
            defaultRestSeconds = 90,
            illustrationPose = "pull"
        ),
        ExerciseDefinition(
            id = "ex_db_row",
            name = "Single-Arm Dumbbell Row",
            muscleGroup = "Back",
            equipment = "Dumbbells",
            level = "beginner",
            metValue = 4.5,
            instructions = "Brace one hand on a bench with neutral spine, and sweep dumbbell toward your hip pocket.",
            defaultSets = 3,
            defaultReps = 10,
            secondaryMuscles = listOf("Biceps", "Forearms", "Abs"),
            steps = listOf(
                "Support one knee and palm on a flat bench with level hips.",
                "Let shoulder blade protract smoothly at the bottom for full lat stretch.",
                "Pull dumbbell back toward hip rather than straight up to shoulder."
            ),
            techniqueGuidance = "Avoid twisting torso open at the top—keep ribs square to the floor.",
            commonMistakes = listOf("Pulling straight to armpit (shrugging)", "Twisting spine excessively"),
            alternativeIds = listOf("ex_barbell_row", "ex_seated_cable_row", "ex_band_row"),
            defaultRepMin = 8,
            defaultRepMax = 12,
            defaultRestSeconds = 75,
            illustrationPose = "row"
        ),
        ExerciseDefinition(
            id = "ex_seated_cable_row",
            name = "Seated Cable Row",
            muscleGroup = "Back",
            equipment = "Cables",
            level = "beginner",
            metValue = 4.3,
            instructions = "Sit tall with knees slightly bent, pull V-handle to lower abdomen while squeezing shoulder blades together.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Biceps", "Shoulders", "Forearms"),
            steps = listOf(
                "Brace feet on platform with tall chest and neutral spine.",
                "Row handle toward navel without leaning far backward.",
                "Allow shoulder blades to glide forward smoothly on the eccentric."
            ),
            techniqueGuidance = "Pause for 1 second with shoulder blades retracted on every rep.",
            commonMistakes = listOf("Rocking torso back and forth", "Shrugging shoulders toward ears"),
            alternativeIds = listOf("ex_barbell_row", "ex_band_row", "ex_db_row"),
            defaultRepMin = 10,
            defaultRepMax = 14,
            defaultRestSeconds = 75,
            illustrationPose = "row"
        ),
        ExerciseDefinition(
            id = "ex_band_row",
            name = "Resistance Band Seated Row",
            muscleGroup = "Back",
            equipment = "Resistance bands",
            level = "beginner",
            metValue = 3.5,
            instructions = "Loop band around secure post or arches of feet while seated tall, and row elbows past ribs.",
            defaultSets = 3,
            defaultReps = 15,
            secondaryMuscles = listOf("Biceps", "Forearms"),
            steps = listOf(
                "Sit upright with legs extended and band looped cleanly around arches.",
                "Pull handles to ribs while keeping elbows close to torso.",
                "Resist the band smoothly back to full reach."
            ),
            techniqueGuidance = "Double-wrap the band for higher mid-back tension on lighter bands.",
            commonMistakes = listOf("Hunching shoulders forward", "Band slipping off shoes—keep toes pointed slightly up"),
            alternativeIds = listOf("ex_seated_cable_row", "ex_db_row"),
            defaultRepMin = 12,
            defaultRepMax = 18,
            defaultRestSeconds = 60,
            illustrationPose = "row"
        ),

        // SHOULDERS
        ExerciseDefinition(
            id = "ex_ohp",
            name = "Overhead Barbell Press",
            muscleGroup = "Shoulders",
            equipment = "Barbell",
            level = "intermediate",
            metValue = 5.2,
            instructions = "Brace glutes and core, press barbell straight vertically past your face and lock out stacked over mid-foot.",
            defaultSets = 4,
            defaultReps = 8,
            secondaryMuscles = listOf("Triceps", "Abs"),
            steps = listOf(
                "Rack bar across front delts with forearms vertical and grip just outside shoulders.",
                "Squeeze glutes and brace abs so lower back does not hyperextend.",
                "Tuck chin slightly as bar passes face, then push head through at top."
            ),
            techniqueGuidance = "Stacked joints (wrists over elbows over shoulders over hips) maximize pressing leverage.",
            commonMistakes = listOf("Excessive lower back arch", "Flaring wrists backward"),
            alternativeIds = listOf("ex_db_shoulder_press", "ex_machine_shoulder_press", "ex_band_ohp"),
            defaultRepMin = 6,
            defaultRepMax = 10,
            defaultRestSeconds = 120,
            illustrationPose = "press"
        ),
        ExerciseDefinition(
            id = "ex_db_shoulder_press",
            name = "Seated Dumbbell Shoulder Press",
            muscleGroup = "Shoulders",
            equipment = "Dumbbells",
            level = "beginner",
            metValue = 4.6,
            instructions = "Sit upright with elbows slightly forward in the scapular plane and press dumbbells overhead without clanking.",
            defaultSets = 3,
            defaultReps = 10,
            secondaryMuscles = listOf("Triceps"),
            steps = listOf(
                "Keep elbows angled ~30 degrees forward rather than straight out to sides.",
                "Press smoothly upward until arms nearly lock.",
                "Lower dumbbells to ear/upper-shoulder height under control."
            ),
            techniqueGuidance = "Pressing in the scapular plane feels natural and reduces shoulder impingement.",
            commonMistakes = listOf("Flaring elbows straight sideways", "Banging dumbbells together overhead"),
            alternativeIds = listOf("ex_ohp", "ex_machine_shoulder_press", "ex_band_ohp"),
            defaultRepMin = 8,
            defaultRepMax = 12,
            defaultRestSeconds = 90,
            illustrationPose = "press"
        ),
        ExerciseDefinition(
            id = "ex_lateral_raise",
            name = "Dumbbell Lateral Raise",
            muscleGroup = "Shoulders",
            equipment = "Dumbbells",
            level = "beginner",
            metValue = 3.5,
            instructions = "Raise dumbbells out to sides in a slight forward arc up to shoulder height, leading with elbows.",
            defaultSets = 3,
            defaultReps = 15,
            secondaryMuscles = listOf("Forearms"),
            steps = listOf(
                "Stand tall with slight hinge and dumbbells just in front of thighs.",
                "Sweep weights outward toward the side walls as if pouring nothing.",
                "Pause briefly at shoulder height and lower slowly for 2–3 seconds."
            ),
            techniqueGuidance = "Use moderate weight so traps don't hijack the movement.",
            commonMistakes = listOf("Swinging hips to heave heavy weights", "Shrugging shoulders upward"),
            alternativeIds = listOf("ex_cable_lateral_raise", "ex_band_ohp"),
            defaultRepMin = 12,
            defaultRepMax = 18,
            defaultRestSeconds = 60,
            illustrationPose = "raise"
        ),
        ExerciseDefinition(
            id = "ex_face_pull",
            name = "Cable Face Pull",
            muscleGroup = "Shoulders",
            equipment = "Cables",
            level = "beginner",
            metValue = 3.5,
            instructions = "Pull rope attachment toward eye level while externally rotating forearms into a double-biceps finish.",
            defaultSets = 3,
            defaultReps = 15,
            secondaryMuscles = listOf("Back"),
            steps = listOf(
                "Set pulley to upper-chest or eye height and grip rope with thumbs pointing back.",
                "Pull toward your forehead/ears while pulling rope ends apart.",
                "Finish with wrists stacked over elbows for rear-delt and rotator-cuff work."
            ),
            techniqueGuidance = "Light to moderate loads with clean external rotation protect shoulder health.",
            commonMistakes = listOf("Using too much weight and leaning back", "Leading with elbows too low"),
            alternativeIds = listOf("ex_lateral_raise", "ex_seated_cable_row"),
            defaultRepMin = 12,
            defaultRepMax = 18,
            defaultRestSeconds = 60,
            illustrationPose = "row"
        ),
        ExerciseDefinition(
            id = "ex_band_ohp",
            name = "Resistance Band Overhead Press",
            muscleGroup = "Shoulders",
            equipment = "Resistance bands",
            level = "beginner",
            metValue = 3.8,
            instructions = "Stand on resistance band with feet hip-width, start handles at collarbone, and press overhead smoothly.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Triceps", "Abs"),
            steps = listOf(
                "Step securely onto center of band with both feet.",
                "Brace core so ribcage stays down during overhead press.",
                "Lower slowly against elastic pull."
            ),
            techniqueGuidance = "Step wider on the band to increase pressing tension without changing bands.",
            commonMistakes = listOf("Rib flare at lockout", "Asymmetric foot placement on band"),
            alternativeIds = listOf("ex_db_shoulder_press", "ex_ohp"),
            defaultRepMin = 10,
            defaultRepMax = 15,
            defaultRestSeconds = 60,
            illustrationPose = "press"
        ),

        // BICEPS
        ExerciseDefinition(
            id = "ex_bicep_curl",
            name = "Dumbbell Bicep Curl",
            muscleGroup = "Biceps",
            equipment = "Dumbbells",
            level = "beginner",
            metValue = 3.5,
            instructions = "Keep elbows pinned beside your ribs, supinate palms upward, and curl smoothly without body swing.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Forearms"),
            steps = listOf(
                "Stand tall with dumbbells at sides and elbows locked next to torso.",
                "Curl up while turning pinkies slightly higher than thumbs at peak.",
                "Lower through full extension for a controlled 2-second eccentric."
            ),
            techniqueGuidance = "Full elbow extension at the bottom maximizes biceps stimulus across the whole ROM.",
            commonMistakes = listOf("Swinging hips to initiate curl", "Dropping elbows backward or flaring wide"),
            alternativeIds = listOf("ex_barbell_curl", "ex_cable_curl", "ex_hammer_curl"),
            defaultRepMin = 10,
            defaultRepMax = 14,
            defaultRestSeconds = 60,
            illustrationPose = "curl"
        ),
        ExerciseDefinition(
            id = "ex_barbell_curl",
            name = "Barbell EZ/Straight Curl",
            muscleGroup = "Biceps",
            equipment = "Barbell",
            level = "intermediate",
            metValue = 4.0,
            instructions = "Grip bar shoulder-width, keep torso braced, and curl bar in a strict arc to upper chest height.",
            defaultSets = 3,
            defaultReps = 10,
            secondaryMuscles = listOf("Forearms"),
            steps = listOf(
                "Brace glutes and abs to keep torso steady.",
                "Curl bar smoothly while keeping upper arms perpendicular to the floor.",
                "Pause briefly near top and lower under control."
            ),
            techniqueGuidance = "An EZ-bar reduces wrist pronation torque if straight bars feel tight.",
            commonMistakes = listOf("Leaning backward at top", "Half-extension at bottom"),
            alternativeIds = listOf("ex_bicep_curl", "ex_cable_curl"),
            defaultRepMin = 8,
            defaultRepMax = 12,
            defaultRestSeconds = 75,
            illustrationPose = "curl"
        ),
        ExerciseDefinition(
            id = "ex_cable_curl",
            name = "Cable Rope/Bar Curl",
            muscleGroup = "Biceps",
            equipment = "Cables",
            level = "beginner",
            metValue = 3.5,
            instructions = "Stand one step back from low pulley for constant tension and curl attachment cleanly toward shoulders.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Forearms"),
            steps = listOf(
                "Attach straight bar or rope at lowest pulley setting.",
                "Keep shoulders down and elbows tucked at sides.",
                "Squeeze biceps for 1 second at top before lowering slowly."
            ),
            techniqueGuidance = "Cables provide constant resistance even at the bottom stretch position.",
            commonMistakes = listOf("Letting cable pull shoulders forward", "Using momentum"),
            alternativeIds = listOf("ex_bicep_curl", "ex_hammer_curl"),
            defaultRepMin = 10,
            defaultRepMax = 15,
            defaultRestSeconds = 60,
            illustrationPose = "curl"
        ),

        // TRICEPS
        ExerciseDefinition(
            id = "ex_tricep_pushdown",
            name = "Cable Tricep Pushdown",
            muscleGroup = "Triceps",
            equipment = "Cables",
            level = "beginner",
            metValue = 3.5,
            instructions = "Lock upper arms against ribs, hinge only at elbows, and extend rope or bar downward to full lockout.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Forearms"),
            steps = listOf(
                "Pin elbows firmly to sides with slight forward torso lean.",
                "Extend forearms downward and flare rope ends apart slightly at bottom.",
                "Control return until forearms reach ~90–100 degrees."
            ),
            techniqueGuidance = "Keep upper arms frozen in place so lateral and medial triceps heads do the work.",
            commonMistakes = listOf("Pushing weight down with shoulders/bodyweight", "Elbows drifting far forward"),
            alternativeIds = listOf("ex_oh_tricep_ext", "ex_close_grip_pushup"),
            defaultRepMin = 10,
            defaultRepMax = 15,
            defaultRestSeconds = 60,
            illustrationPose = "extension"
        ),
        ExerciseDefinition(
            id = "ex_oh_tricep_ext",
            name = "Overhead Dumbbell Tricep Extension",
            muscleGroup = "Triceps",
            equipment = "Dumbbells",
            level = "intermediate",
            metValue = 3.8,
            instructions = "Hold one dumbbell overhead with both palms, keep elbows pointed forward, and lower behind head for a deep stretch.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Shoulders"),
            steps = listOf(
                "Brace core so ribs stay stacked over hips.",
                "Lower weight behind head until forearms touch biceps comfortably.",
                "Drive elbows into full extension overhead."
            ),
            techniqueGuidance = "Long-head triceps growth thrives on the deep overhead stretch position.",
            commonMistakes = listOf("Flaring elbows wide outward", "Arching lower back"),
            alternativeIds = listOf("ex_tricep_pushdown", "ex_close_grip_pushup"),
            defaultRepMin = 10,
            defaultRepMax = 14,
            defaultRestSeconds = 75,
            illustrationPose = "extension"
        ),
        ExerciseDefinition(
            id = "ex_close_grip_pushup",
            name = "Close-Grip Tricep Push-Ups",
            muscleGroup = "Triceps",
            equipment = "Bodyweight",
            level = "intermediate",
            metValue = 4.2,
            instructions = "Set hands inside shoulder width, tuck elbows tight along ribs, and press cleanly through triceps.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Chest", "Abs"),
            steps = listOf(
                "Place thumbs roughly chest-width apart in a strong plank.",
                "Brush elbows past ribcage as you lower chest toward hands.",
                "Press forcefully to full arm lockout."
            ),
            techniqueGuidance = "Avoid cramping hands into a tight diamond if wrists feel jammed—shoulder-width or slightly narrower works smoothly.",
            commonMistakes = listOf("Hips sagging", "Elbows flaring outward"),
            alternativeIds = listOf("ex_tricep_pushdown", "ex_oh_tricep_ext"),
            defaultRepMin = 8,
            defaultRepMax = 15,
            defaultRestSeconds = 60,
            illustrationPose = "press"
        ),

        // FOREARMS
        ExerciseDefinition(
            id = "ex_hammer_curl",
            name = "Hammer Curl & Brachioradialis",
            muscleGroup = "Forearms",
            equipment = "Dumbbells",
            level = "beginner",
            metValue = 3.5,
            instructions = "Use a neutral palms-facing grip and curl dumbbells smoothly to build forearms and brachialis thickness.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Biceps"),
            steps = listOf(
                "Hold dumbbells at sides with thumbs pointing straight forward.",
                "Curl smoothly to shoulder height without twisting wrists.",
                "Lower slowly for 2 seconds to full arm length."
            ),
            techniqueGuidance = "Squeeze the dumbbell handle firmly throughout the set to maximize forearm activation.",
            commonMistakes = listOf("Body swing", "Rotating palms upward mid-rep"),
            alternativeIds = listOf("ex_farmers_carry", "ex_wrist_curl"),
            defaultRepMin = 10,
            defaultRepMax = 15,
            defaultRestSeconds = 60,
            illustrationPose = "curl"
        ),
        ExerciseDefinition(
            id = "ex_farmers_carry",
            name = "Heavy Farmer's Carry (Timed/Steps)",
            muscleGroup = "Forearms",
            equipment = "Kettlebells",
            level = "beginner",
            metValue = 5.0,
            instructions = "Grip heavy kettlebells or dumbbells with tall posture, pack shoulders down, and walk with crisp controlled steps.",
            defaultSets = 3,
            defaultReps = 30,
            secondaryMuscles = listOf("Abs", "Back", "Shoulders"),
            steps = listOf(
                "Deadlift weights safely with flat back.",
                "Walk briskly with ribs stacked and zero side-to-side sway.",
                "Set down cleanly with hip hinge when time/steps complete."
            ),
            techniqueGuidance = "Treat each rep count as seconds or meters walked while maintaining crush grip.",
            commonMistakes = listOf("Letting shoulders roll forward", "Rushing steps carelessly"),
            alternativeIds = listOf("ex_hammer_curl", "ex_wrist_curl"),
            defaultRepMin = 20,
            defaultRepMax = 40,
            defaultRestSeconds = 75,
            illustrationPose = "hinge"
        ),
        ExerciseDefinition(
            id = "ex_wrist_curl",
            name = "Barbell Forearm Wrist Curl",
            muscleGroup = "Forearms",
            equipment = "Barbell",
            level = "beginner",
            metValue = 2.8,
            instructions = "Rest forearms along knees or bench with palms up, lower bar to fingertips, and curl wrists smoothly upward.",
            defaultSets = 3,
            defaultReps = 15,
            secondaryMuscles = emptyList(),
            steps = listOf(
                "Rest forearms on bench so only wrists hang past the edge.",
                "Allow bar to roll subtly down into fingers, then close grip and flex wrists up.",
                "Lower slowly without letting elbows lift."
            ),
            techniqueGuidance = "Controlled high-rep sets build tendon resilience and grip endurance safely.",
            commonMistakes = listOf("Using excessive weight and straining tendons", "Lifting forearms off bench"),
            alternativeIds = listOf("ex_hammer_curl", "ex_farmers_carry"),
            defaultRepMin = 12,
            defaultRepMax = 20,
            defaultRestSeconds = 45,
            illustrationPose = "curl"
        ),

        // QUADRICEPS
        ExerciseDefinition(
            id = "ex_squat",
            name = "Barbell Back Squat",
            muscleGroup = "Quadriceps",
            equipment = "Barbell",
            level = "intermediate",
            metValue = 6.5,
            instructions = "Brace core deeply, sit hips down between knees until thighs reach parallel or deeper, and drive through mid-foot.",
            defaultSets = 4,
            defaultReps = 8,
            secondaryMuscles = listOf("Glutes", "Hamstrings", "Abs"),
            steps = listOf(
                "Set bar across upper traps/rear delts and take a big 360-degree breath into belly.",
                "Break at hips and knees together while tracking knees over toes.",
                "Reach parallel crease-below-knee depth, then drive shoulders and hips up evenly."
            ),
            techniqueGuidance = "Keep weight balanced across tripod foot (heel, big toe, pinky toe).",
            commonMistakes = listOf("Knees caving inward", "Heels lifting off floor", "Chest collapsing forward out of bottom"),
            alternativeIds = listOf("ex_goblet_squat", "ex_leg_press", "ex_lunges"),
            defaultRepMin = 5,
            defaultRepMax = 8,
            defaultRestSeconds = 150,
            illustrationPose = "squat"
        ),
        ExerciseDefinition(
            id = "ex_goblet_squat",
            name = "Dumbbell Goblet Squat",
            muscleGroup = "Quadriceps",
            equipment = "Dumbbells",
            level = "beginner",
            metValue = 5.0,
            instructions = "Cup one dumbbell vertically tight against upper chest, keep torso tall, and squat deep between elbows.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Glutes", "Abs"),
            steps = listOf(
                "Hold dumbbell snugly against sternum with elbows tucked down.",
                "Drop hips smoothly between heels while keeping chest proud.",
                "Drive knees gently outward and stand tall."
            ),
            techniqueGuidance = "Front counterbalance naturally teaches ideal upright squat mechanics.",
            commonMistakes = listOf("Holding dumbbell far out from chest", "Rounding lower back at bottom"),
            alternativeIds = listOf("ex_squat", "ex_leg_press", "ex_kb_goblet_squat"),
            defaultRepMin = 10,
            defaultRepMax = 15,
            defaultRestSeconds = 90,
            illustrationPose = "squat"
        ),
        ExerciseDefinition(
            id = "ex_leg_press",
            name = "Machine Leg Press",
            muscleGroup = "Quadriceps",
            equipment = "Machines",
            level = "beginner",
            metValue = 4.8,
            instructions = "Plant feet shoulder-width on sled, lower knees smoothly toward chest without curling hips off pad, and press out.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Glutes", "Hamstrings"),
            steps = listOf(
                "Place feet low-to-mid platform for quad emphasis.",
                "Unlock safeties and control descent for 2 seconds.",
                "Press smoothly without snapping knees into hyperextension."
            ),
            techniqueGuidance = "Stop descent just before your tailbone tucks off the seat cushion.",
            commonMistakes = listOf("Locking knees aggressively", "Lower back peeling off pad at depth"),
            alternativeIds = listOf("ex_squat", "ex_goblet_squat", "ex_lunges"),
            defaultRepMin = 10,
            defaultRepMax = 15,
            defaultRestSeconds = 90,
            illustrationPose = "squat"
        ),
        ExerciseDefinition(
            id = "ex_lunges",
            name = "Walking Dumbbell Lunges",
            muscleGroup = "Quadriceps",
            equipment = "Dumbbells",
            level = "beginner",
            metValue = 5.2,
            instructions = "Step forward into a balanced 90/90 split stance, tap back knee lightly near floor, and drive up through front heel.",
            defaultSets = 3,
            defaultReps = 10,
            secondaryMuscles = listOf("Glutes", "Hamstrings", "Abs"),
            steps = listOf(
                "Keep feet on train-tracks (hip width) rather than a tightrope for balance.",
                "Lower vertically with upright torso until back knee hovers above floor.",
                "Drive forward leg cleanly into the next stride."
            ),
            techniqueGuidance = "Count reps per leg to ensure balanced unilateral strength.",
            commonMistakes = listOf("Stepping on a narrow tightrope", "Bouncing back knee hard into floor"),
            alternativeIds = listOf("ex_goblet_squat", "ex_leg_press"),
            defaultRepMin = 8,
            defaultRepMax = 12,
            defaultRestSeconds = 75,
            illustrationPose = "squat"
        ),

        // HAMSTRINGS
        ExerciseDefinition(
            id = "ex_rdl",
            name = "Barbell Romanian Deadlift",
            muscleGroup = "Hamstrings",
            equipment = "Barbell",
            level = "intermediate",
            metValue = 5.8,
            instructions = "Soften knees slightly, push hips straight back keeping barbell gliding along thighs until deep hamstring stretch, then drive hips forward.",
            defaultSets = 3,
            defaultReps = 10,
            secondaryMuscles = listOf("Glutes", "Back", "Forearms"),
            steps = listOf(
                "Unrack bar, pack lats tight as if squeezing oranges in armpits.",
                "Push hips backward horizontally—stop as soon as hips stop traveling back (mid-shin).",
                "Squeeze glutes to snap hips forward to tall posture without hyperextending."
            ),
            techniqueGuidance = "Keep bar touching your legs throughout the entire hinge.",
            commonMistakes = listOf("Squatting knees forward instead of hinging", "Letting bar drift away from shins"),
            alternativeIds = listOf("ex_leg_curl_machine", "ex_kb_swing", "ex_band_rdl"),
            defaultRepMin = 8,
            defaultRepMax = 12,
            defaultRestSeconds = 120,
            illustrationPose = "hinge"
        ),
        ExerciseDefinition(
            id = "ex_leg_curl_machine",
            name = "Seated/Lying Hamstring Curl",
            muscleGroup = "Hamstrings",
            equipment = "Machines",
            level = "beginner",
            metValue = 3.8,
            instructions = "Align knee joint with machine pivot, lock hips into seat, and curl pad forcefully toward glutes.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Calves"),
            steps = listOf(
                "Adjust ankle pad above Achilles tendon and brace thighs tightly.",
                "Curl cleanly to full knee flexion.",
                "Return slowly for a 3-second stretch."
            ),
            techniqueGuidance = "Seated leg curls train hamstrings in a lengthened hip-flexed position.",
            commonMistakes = listOf("Lifting hips off seat pad", "Dropping weight stack rapidly"),
            alternativeIds = listOf("ex_rdl", "ex_band_rdl"),
            defaultRepMin = 10,
            defaultRepMax = 15,
            defaultRestSeconds = 75,
            illustrationPose = "squat"
        ),
        ExerciseDefinition(
            id = "ex_band_rdl",
            name = "Resistance Band Good Morning / RDL",
            muscleGroup = "Hamstrings",
            equipment = "Resistance bands",
            level = "beginner",
            metValue = 3.6,
            instructions = "Stand on heavy loop band, hinge hips smoothly backward with flat spine, and snap hips tall against band pull.",
            defaultSets = 3,
            defaultReps = 15,
            secondaryMuscles = listOf("Glutes", "Back"),
            steps = listOf(
                "Anchor band securely beneath both feet.",
                "Push hips back with soft knees until torso is ~45 degrees.",
                "Drive glutes forward to standing lockout."
            ),
            techniqueGuidance = "Focus on pushing hips backward rather than bowing your chest down.",
            commonMistakes = listOf("Rounding upper back", "Bending knees too much"),
            alternativeIds = listOf("ex_rdl", "ex_kb_swing"),
            defaultRepMin = 12,
            defaultRepMax = 18,
            defaultRestSeconds = 60,
            illustrationPose = "hinge"
        ),

        // GLUTES
        ExerciseDefinition(
            id = "ex_hip_thrust",
            name = "Barbell Hip Thrust",
            muscleGroup = "Glutes",
            equipment = "Barbell",
            level = "intermediate",
            metValue = 5.2,
            instructions = "Rest upper shoulder blades on bench edge, tuck chin slightly, and drive hips upward until shins are vertical at lockout.",
            defaultSets = 4,
            defaultReps = 10,
            secondaryMuscles = listOf("Hamstrings", "Quadriceps"),
            steps = listOf(
                "Place padded barbell across hip crease with feet shoulder-width.",
                "Keep ribs tucked down and look forward—not at the ceiling.",
                "Pause 1 full second at horizontal lockout squeezing glutes hard."
            ),
            techniqueGuidance = "Vertical shins at the top keep tension on glutes instead of quads or hamstrings.",
            commonMistakes = listOf("Hyperextending lower back at top", "Feet placed too close or too far"),
            alternativeIds = listOf("ex_glute_bridge", "ex_kb_swing"),
            defaultRepMin = 8,
            defaultRepMax = 12,
            defaultRestSeconds = 90,
            illustrationPose = "hinge"
        ),
        ExerciseDefinition(
            id = "ex_glute_bridge",
            name = "Bodyweight Single/Double Glute Bridge",
            muscleGroup = "Glutes",
            equipment = "Bodyweight",
            level = "beginner",
            metValue = 3.5,
            instructions = "Lie supine with heels near glutes, brace abs, and press hips upward into a straight knee-to-shoulder bridge.",
            defaultSets = 3,
            defaultReps = 15,
            secondaryMuscles = listOf("Hamstrings", "Abs"),
            steps = listOf(
                "Press lower back gently into floor to start from posterior tilt.",
                "Drive through heels to elevate hips cleanly.",
                "Hold peak contraction for 1–2 seconds each rep."
            ),
            techniqueGuidance = "Progress to single-leg bridges once 15–20 double-leg reps feel effortless.",
            commonMistakes = listOf("Flaring ribs and arching lower back", "Pushing through toes"),
            alternativeIds = listOf("ex_hip_thrust", "ex_kb_swing"),
            defaultRepMin = 12,
            defaultRepMax = 20,
            defaultRestSeconds = 60,
            illustrationPose = "hinge"
        ),
        ExerciseDefinition(
            id = "ex_kb_swing",
            name = "Kettlebell Swing",
            muscleGroup = "Glutes",
            equipment = "Kettlebells",
            level = "intermediate",
            metValue = 8.0,
            instructions = "Explosive hip hinge snapping hips tall to float kettlebell to chest height while arms act strictly as tethers.",
            defaultSets = 4,
            defaultReps = 15,
            secondaryMuscles = listOf("Hamstrings", "Abs", "Back"),
            steps = listOf(
                "Hike kettlebell high between upper thighs with flat spine.",
                "Snap glutes and quads tight at top as if standing into a vertical plank.",
                "Wait until bell nearly touches hips before hinging back."
            ),
            techniqueGuidance = "All power comes from hip snap—never lift the bell with front shoulders.",
            commonMistakes = listOf("Squatting down instead of hinging", "Hinging too early while bell is still high"),
            alternativeIds = listOf("ex_rdl", "ex_hip_thrust"),
            defaultRepMin = 12,
            defaultRepMax = 20,
            defaultRestSeconds = 75,
            illustrationPose = "hinge"
        ),

        // CALVES
        ExerciseDefinition(
            id = "ex_standing_calf_raise",
            name = "Machine Standing Calf Raise",
            muscleGroup = "Calves",
            equipment = "Machines",
            level = "beginner",
            metValue = 3.2,
            instructions = "Lower heels fully below step for a 2-second stretch, then rise smoothly onto balls of feet and pause at peak.",
            defaultSets = 4,
            defaultReps = 15,
            secondaryMuscles = emptyList(),
            steps = listOf(
                "Keep knees nearly straight (micro-bend) to target gastrocnemius.",
                "Pause 1–2 seconds at the bottom stretch so Achilles tendon doesn't bounce reps.",
                "Drive vertically over big and second toes."
            ),
            techniqueGuidance = "Slow tempo eliminates tendon bounce and builds real calf hypertrophy.",
            commonMistakes = listOf("Bouncing rapidly at bottom", "Rolling outward onto pinky toes"),
            alternativeIds = listOf("ex_single_leg_calf_raise"),
            defaultRepMin = 12,
            defaultRepMax = 20,
            defaultRestSeconds = 45,
            illustrationPose = "squat"
        ),
        ExerciseDefinition(
            id = "ex_single_leg_calf_raise",
            name = "Single-Leg Bodyweight Calf Raise",
            muscleGroup = "Calves",
            equipment = "Bodyweight",
            level = "beginner",
            metValue = 3.0,
            instructions = "Stand on a step edge with light fingertip balance support, drop heel deeply, and press all the way onto your forefoot.",
            defaultSets = 3,
            defaultReps = 15,
            secondaryMuscles = emptyList(),
            steps = listOf(
                "Use a wall or rail solely for light balance, not to pull yourself up.",
                "Sink heel deep for a full 2-second stretch.",
                "Hold 1 second at top contraction."
            ),
            techniqueGuidance = "Add a dumbbell in one hand once 15–20 strict reps per leg become comfortable.",
            commonMistakes = listOf("Shortening range of motion", "Rushing tempo"),
            alternativeIds = listOf("ex_standing_calf_raise"),
            defaultRepMin = 12,
            defaultRepMax = 20,
            defaultRestSeconds = 45,
            illustrationPose = "squat"
        ),

        // ABS
        ExerciseDefinition(
            id = "ex_plank",
            name = "Forearm Plank (Seconds)",
            muscleGroup = "Abs",
            equipment = "Bodyweight",
            level = "beginner",
            metValue = 3.5,
            instructions = "Press forearms into mat, tuck tailbone slightly, squeeze glutes, and hold rigid alignment for target seconds.",
            defaultSets = 3,
            defaultReps = 45,
            secondaryMuscles = listOf("Shoulders", "Glutes"),
            steps = listOf(
                "Stack elbows directly beneath shoulders.",
                "Pull elbows mentally toward toes to engage anterior core.",
                "Breathe steadily behind a braced abdominal wall."
            ),
            techniqueGuidance = "Quality tension beats endless sagging holds—stop the set if your lower back dips.",
            commonMistakes = listOf("Hips sagging toward mat", "Hiking hips too high"),
            alternativeIds = listOf("ex_hanging_leg_raise", "ex_cable_crunch", "ex_russian_twist"),
            defaultRepMin = 30,
            defaultRepMax = 60,
            defaultRestSeconds = 45,
            illustrationPose = "core"
        ),
        ExerciseDefinition(
            id = "ex_hanging_leg_raise",
            name = "Hanging Knee / Leg Raise",
            muscleGroup = "Abs",
            equipment = "Bodyweight",
            level = "intermediate",
            metValue = 4.5,
            instructions = "Hang from bar with active shoulders and curl pelvis upward toward ribcage without swinging.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Forearms", "Back"),
            steps = listOf(
                "Keep shoulders slightly depressed so body doesn't pendulum.",
                "Focus on curling tailbone forward/upward rather than only lifting knees with hip flexors.",
                "Lower slowly to a dead stop at bottom."
            ),
            techniqueGuidance = "Rounding the lower spine upward is what truly contracts the rectus abdominis.",
            commonMistakes = listOf("Swinging legs with momentum", "Leaving pelvis neutral and only flexing hips"),
            alternativeIds = listOf("ex_cable_crunch", "ex_plank"),
            defaultRepMin = 8,
            defaultRepMax = 15,
            defaultRestSeconds = 60,
            illustrationPose = "core"
        ),
        ExerciseDefinition(
            id = "ex_cable_crunch",
            name = "Kneeling Cable Rope Crunch",
            muscleGroup = "Abs",
            equipment = "Cables",
            level = "intermediate",
            metValue = 3.8,
            instructions = "Anchor rope against forehead/temples, keep hips stationary above knees, and curl ribcage downward toward pelvis.",
            defaultSets = 3,
            defaultReps = 15,
            secondaryMuscles = emptyList(),
            steps = listOf(
                "Kneel smoothly back from high pulley with rope held firmly near ears.",
                "Flex spine downward by bringing ribs toward hips—do not sit back onto heels.",
                "Return up under control to a gentle ab stretch."
            ),
            techniqueGuidance = "Easy to load progressively over months just like any other muscle group.",
            commonMistakes = listOf("Hinging at the hips instead of flexing the spine", "Pulling rope with arms"),
            alternativeIds = listOf("ex_hanging_leg_raise", "ex_russian_twist"),
            defaultRepMin = 12,
            defaultRepMax = 18,
            defaultRestSeconds = 60,
            illustrationPose = "core"
        ),
        ExerciseDefinition(
            id = "ex_russian_twist",
            name = "Weighted Russian Twist",
            muscleGroup = "Abs",
            equipment = "Dumbbells",
            level = "beginner",
            metValue = 3.8,
            instructions = "Lean torso to ~45 degrees with long spine and rotate ribcage smoothly side to side.",
            defaultSets = 3,
            defaultReps = 16,
            secondaryMuscles = emptyList(),
            steps = listOf(
                "Keep chest lifted so lower back stays long and braced.",
                "Rotate shoulders and ribcage rather than waving arms alone.",
                "Keep feet lightly planted if lower back fatigues."
            ),
            techniqueGuidance = "Total reps count alternate sides smoothly.",
            commonMistakes = listOf("Rounding spine aggressively", "Moving only hands without torso rotation"),
            alternativeIds = listOf("ex_plank", "ex_cable_crunch"),
            defaultRepMin = 14,
            defaultRepMax = 24,
            defaultRestSeconds = 45,
            illustrationPose = "core"
        ),

        // FULL BODY
        ExerciseDefinition(
            id = "ex_burpees",
            name = "Athletic Burpees",
            muscleGroup = "Full Body",
            equipment = "Bodyweight",
            level = "intermediate",
            metValue = 8.5,
            instructions = "Squat cleanly, shoot feet to solid plank, step or snap feet outside hands, and explode upward.",
            defaultSets = 3,
            defaultReps = 12,
            secondaryMuscles = listOf("Chest", "Quadriceps", "Abs", "Shoulders"),
            steps = listOf(
                "Plant palms cleanly shoulder-width apart.",
                "Hold rigid plank without sagging hips.",
                "Land softly with bent knees on every jump."
            ),
            techniqueGuidance = "Step back/forward rather than jumping to scale down impact while keeping pace high.",
            commonMistakes = listOf("Worming lower back off floor", "Landing stiff-legged"),
            alternativeIds = listOf("ex_kb_swing", "ex_thruster"),
            defaultRepMin = 8,
            defaultRepMax = 15,
            defaultRestSeconds = 60,
            illustrationPose = "cardio"
        ),
        ExerciseDefinition(
            id = "ex_thruster",
            name = "Kettlebell / Dumbbell Thruster",
            muscleGroup = "Full Body",
            equipment = "Kettlebells",
            level = "advanced",
            metValue = 7.5,
            instructions = "Rack weights at shoulders, drop into full depth squat, and use leg drive to press cleanly overhead.",
            defaultSets = 3,
            defaultReps = 10,
            secondaryMuscles = listOf("Quadriceps", "Glutes", "Shoulders", "Triceps", "Abs"),
            steps = listOf(
                "Hold weights cleanly in front rack with high elbows.",
                "Squat smoothly to parallel while keeping heels down.",
                "Accelerate out of squat directly into overhead lockout."
            ),
            techniqueGuidance = "Exhale sharply as you drive weights overhead to stabilize your core.",
            commonMistakes = listOf("Pressing before hips finish extending", "Dropping elbows during squat"),
            alternativeIds = listOf("ex_kb_swing", "ex_burpees", "ex_goblet_squat"),
            defaultRepMin = 8,
            defaultRepMax = 12,
            defaultRestSeconds = 90,
            illustrationPose = "squat"
        ),
        ExerciseDefinition(
            id = "ex_deadlift",
            name = "Conventional Barbell Deadlift",
            muscleGroup = "Full Body",
            equipment = "Barbell",
            level = "advanced",
            metValue = 7.0,
            instructions = "Set bar over mid-foot, pull slack out with tight lats, push floor away with legs, and lock hips tall cleanly.",
            defaultSets = 3,
            defaultReps = 5,
            secondaryMuscles = listOf("Hamstrings", "Glutes", "Back", "Forearms", "Abs"),
            steps = listOf(
                "Walk shins ~1 inch from bar, grip outside knees, and drop shins to touch bar.",
                "Lift chest and lock lats down before pulling off the floor.",
                "Keep bar dragging lightly along legs all the way to lockout."
            ),
            techniqueGuidance = "Pulling the slack out of the bar before leaving the floor keeps your lumbar spine locked solid.",
            commonMistakes = listOf("Yanking bar off floor with rounded back", "Starting with hips too low like a squat"),
            alternativeIds = listOf("ex_rdl", "ex_hip_thrust"),
            defaultRepMin = 3,
            defaultRepMax = 6,
            defaultRestSeconds = 180,
            illustrationPose = "hinge"
        )
    )

    fun findExerciseById(id: String): ExerciseDefinition? =
        exercises.firstOrNull { it.id == id }

    fun findExerciseByName(name: String): ExerciseDefinition? =
        exercises.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun alternativesFor(exercise: ExerciseDefinition): List<ExerciseDefinition> {
        val explicit = exercise.alternativeIds.mapNotNull { findExerciseById(it) }
        if (explicit.isNotEmpty()) return explicit
        return exercises.filter { it.id != exercise.id && it.muscleGroup == exercise.muscleGroup }.take(4)
    }

    private fun entry(
        exerciseId: String,
        sets: Long,
        repMin: Long,
        repMax: Long,
        weightKg: Double,
        restSeconds: Long,
        setType: String = "working",
        notes: String = ""
    ): WorkoutExerciseEntry {
        val def = findExerciseById(exerciseId) ?: exercises.first()
        return WorkoutExerciseEntry(
            exerciseId = def.id,
            exerciseName = def.name,
            muscleGroup = def.muscleGroup,
            secondaryMuscles = def.secondaryMuscles,
            equipment = def.equipment,
            targetSets = sets,
            targetReps = repMax,
            repRangeMin = repMin,
            repRangeMax = repMax,
            targetWeightKg = weightKg,
            restSeconds = restSeconds,
            setType = setType,
            notes = notes
        )
    }

    private val basePrograms: List<WorkoutProgram> = listOf(
        WorkoutProgram(
            id = "prog_full_body",
            name = "Full Body Athletic Foundation",
            category = "full_body",
            goal = "general_fitness",
            level = "beginner",
            daysPerWeek = 3L,
            description = "Balanced 3-day total-body rotation hitting squat, hinge, horizontal push/pull, overhead press, and core.",
            weeklySplit = listOf("Mon: Full Body A", "Wed: Full Body B", "Fri: Full Body C"),
            progressionGuidance = "Add 1 rep per set up to the top of the rep range, then increase weight by 2.5 kg with clean form.",
            workouts = listOf(
                WorkoutPlan(
                    id = "tpl_full_body_a",
                    name = "Full Body A — Squat & Press",
                    level = "beginner",
                    targetGoal = "general_fitness",
                    programId = "prog_full_body",
                    programName = "Full Body Athletic Foundation",
                    dayLabel = "Day 1",
                    progressionNote = "Target 8–12 controlled reps; leave 1–2 reps in reserve.",
                    exercises = listOf(
                        entry("ex_goblet_squat", 3, 8, 12, 16.0, 90),
                        entry("ex_incline_db_press", 3, 8, 12, 16.0, 90),
                        entry("ex_lat_pulldown", 3, 10, 12, 40.0, 75),
                        entry("ex_rdl", 3, 8, 10, 40.0, 90),
                        entry("ex_plank", 3, 30, 45, 0.0, 45)
                    )
                ),
                WorkoutPlan(
                    id = "tpl_full_body_b",
                    name = "Full Body B — Hinge & Pull",
                    level = "beginner",
                    targetGoal = "general_fitness",
                    programId = "prog_full_body",
                    programName = "Full Body Athletic Foundation",
                    dayLabel = "Day 2",
                    progressionNote = "Focus on crisp hinge bracing and full lat stretch.",
                    exercises = listOf(
                        entry("ex_hip_thrust", 3, 10, 12, 50.0, 90),
                        entry("ex_db_shoulder_press", 3, 8, 12, 12.0, 75),
                        entry("ex_seated_cable_row", 3, 10, 14, 40.0, 75),
                        entry("ex_lunges", 3, 8, 10, 10.0, 75),
                        entry("ex_cable_crunch", 3, 12, 16, 25.0, 60)
                    )
                )
            )
        ),
        WorkoutProgram(
            id = "prog_upper_lower",
            name = "Upper / Lower Strength & Size",
            category = "upper_lower",
            goal = "muscle_gain",
            level = "intermediate",
            daysPerWeek = 4L,
            description = "Classic 4-day split maximizing compound intensity and 48–72 hour regional recovery.",
            weeklySplit = listOf("Mon: Upper A", "Tue: Lower A", "Thu: Upper B", "Fri: Lower B"),
            progressionGuidance = "Track top working set each session. Confirm +2.5 kg progression once all working sets hit top rep target.",
            workouts = listOf(
                WorkoutPlan(
                    id = "tpl_upper_strength",
                    name = "Upper A — Heavy Press & Row",
                    level = "intermediate",
                    targetGoal = "muscle_gain",
                    programId = "prog_upper_lower",
                    programName = "Upper / Lower Strength & Size",
                    dayLabel = "Upper A",
                    progressionNote = "Compounds 6–10 reps (120s rest); accessories 10–14 reps (60–75s rest).",
                    exercises = listOf(
                        entry("ex_bench_press", 4, 6, 8, 60.0, 120),
                        entry("ex_barbell_row", 4, 6, 10, 55.0, 120),
                        entry("ex_db_shoulder_press", 3, 8, 12, 16.0, 90),
                        entry("ex_lat_pulldown", 3, 10, 12, 45.0, 75),
                        entry("ex_bicep_curl", 3, 10, 14, 12.0, 60),
                        entry("ex_tricep_pushdown", 3, 10, 14, 22.0, 60)
                    )
                ),
                WorkoutPlan(
                    id = "tpl_lower_power",
                    name = "Lower A — Squat & Posterior",
                    level = "intermediate",
                    targetGoal = "muscle_gain",
                    programId = "prog_upper_lower",
                    programName = "Upper / Lower Strength & Size",
                    dayLabel = "Lower A",
                    progressionNote = "Maintain bracing depth on squats and hamstring stretch on RDL.",
                    exercises = listOf(
                        entry("ex_squat", 4, 6, 8, 70.0, 150),
                        entry("ex_rdl", 3, 8, 10, 60.0, 120),
                        entry("ex_leg_press", 3, 10, 14, 100.0, 90),
                        entry("ex_leg_curl_machine", 3, 10, 14, 35.0, 75),
                        entry("ex_standing_calf_raise", 4, 12, 18, 40.0, 45)
                    )
                )
            )
        ),
        WorkoutProgram(
            id = "prog_ppl",
            name = "Push / Pull / Legs (PPL)",
            category = "ppl",
            goal = "muscle_gain",
            level = "intermediate",
            daysPerWeek = 6L,
            description = "High-frequency biomechanical split grouping pressing, pulling, and lower-body muscles cleanly.",
            weeklySplit = listOf("Day 1: Push", "Day 2: Pull", "Day 3: Legs", "Day 4: Rest or Repeat"),
            progressionGuidance = "Prioritize clean double progression on primary lifts before adding accessory volume.",
            workouts = listOf(
                WorkoutPlan(
                    id = "tpl_ppl_push",
                    name = "Push — Chest, Delts & Triceps",
                    level = "intermediate",
                    targetGoal = "muscle_gain",
                    programId = "prog_ppl",
                    programName = "Push / Pull / Legs (PPL)",
                    dayLabel = "Push",
                    exercises = listOf(
                        entry("ex_bench_press", 4, 6, 10, 60.0, 120),
                        entry("ex_incline_db_press", 3, 8, 12, 20.0, 90),
                        entry("ex_ohp", 3, 8, 10, 35.0, 90),
                        entry("ex_lateral_raise", 3, 12, 16, 8.0, 60),
                        entry("ex_tricep_pushdown", 3, 10, 14, 25.0, 60)
                    )
                ),
                WorkoutPlan(
                    id = "tpl_ppl_pull",
                    name = "Pull — Back, Rear Delts & Biceps",
                    level = "intermediate",
                    targetGoal = "muscle_gain",
                    programId = "prog_ppl",
                    programName = "Push / Pull / Legs (PPL)",
                    dayLabel = "Pull",
                    exercises = listOf(
                        entry("ex_pullups", 3, 6, 10, 0.0, 120),
                        entry("ex_barbell_row", 4, 8, 10, 55.0, 120),
                        entry("ex_seated_cable_row", 3, 10, 12, 45.0, 75),
                        entry("ex_face_pull", 3, 12, 16, 18.0, 60),
                        entry("ex_bicep_curl", 3, 10, 14, 12.0, 60),
                        entry("ex_hammer_curl", 3, 10, 14, 12.0, 60)
                    )
                ),
                WorkoutPlan(
                    id = "tpl_ppl_legs",
                    name = "Legs — Quads, Hams, Glutes & Calves",
                    level = "intermediate",
                    targetGoal = "muscle_gain",
                    programId = "prog_ppl",
                    programName = "Push / Pull / Legs (PPL)",
                    dayLabel = "Legs",
                    exercises = listOf(
                        entry("ex_squat", 4, 6, 8, 70.0, 150),
                        entry("ex_rdl", 3, 8, 10, 60.0, 120),
                        entry("ex_lunges", 3, 8, 12, 14.0, 90),
                        entry("ex_leg_curl_machine", 3, 10, 14, 35.0, 75),
                        entry("ex_standing_calf_raise", 4, 12, 18, 45.0, 45)
                    )
                )
            )
        ),
        WorkoutProgram(
            id = "prog_bro_split",
            name = "Classic Bodypart Split (Bro Split)",
            category = "bro_split",
            goal = "muscle_gain",
            level = "advanced",
            daysPerWeek = 5L,
            description = "Dedicated daily focus for Chest, Back, Shoulders, Arms, and Legs with deep hypertrophy volume.",
            weeklySplit = listOf("Mon: Chest", "Tue: Back", "Wed: Legs", "Thu: Shoulders", "Fri: Arms & Core"),
            progressionGuidance = "Manage systemic fatigue carefully; deload 1 week every 6–8 weeks.",
            workouts = listOf(
                WorkoutPlan(
                    id = "tpl_bro_chest",
                    name = "Chest Focus Day",
                    level = "advanced",
                    targetGoal = "muscle_gain",
                    programId = "prog_bro_split",
                    programName = "Classic Bodypart Split (Bro Split)",
                    dayLabel = "Chest",
                    exercises = listOf(
                        entry("ex_bench_press", 4, 6, 10, 65.0, 120),
                        entry("ex_incline_db_press", 4, 8, 12, 22.0, 90),
                        entry("ex_chest_press_machine", 3, 10, 14, 50.0, 75),
                        entry("ex_cable_fly", 3, 12, 16, 14.0, 60)
                    )
                ),
                WorkoutPlan(
                    id = "tpl_bro_arms",
                    name = "Arms & Forearms Specialization",
                    level = "advanced",
                    targetGoal = "muscle_gain",
                    programId = "prog_bro_split",
                    programName = "Classic Bodypart Split (Bro Split)",
                    dayLabel = "Arms",
                    exercises = listOf(
                        entry("ex_barbell_curl", 3, 8, 12, 25.0, 75),
                        entry("ex_oh_tricep_ext", 3, 10, 12, 18.0, 75),
                        entry("ex_cable_curl", 3, 10, 14, 20.0, 60),
                        entry("ex_tricep_pushdown", 3, 10, 14, 25.0, 60),
                        entry("ex_hammer_curl", 3, 10, 14, 14.0, 60),
                        entry("ex_wrist_curl", 3, 15, 20, 15.0, 45)
                    )
                )
            )
        ),
        WorkoutProgram(
            id = "prog_strength",
            name = "5x5 & Heavy Compound Strength",
            category = "strength",
            goal = "muscle_gain",
            level = "advanced",
            daysPerWeek = 3L,
            description = "Low-rep heavy barbell force production centered around Squat, Bench, Deadlift, Press, and Row.",
            weeklySplit = listOf("Mon: Squat / Bench / Row", "Wed: Deadlift / Press / Pull-Ups", "Fri: Squat / Bench / Row"),
            progressionGuidance = "Rest 2.5–3 minutes between heavy compound sets. Never grind reps with rounded posture.",
            workouts = listOf(
                WorkoutPlan(
                    id = "tpl_strength_a",
                    name = "Strength Day A — Squat & Bench",
                    level = "advanced",
                    targetGoal = "muscle_gain",
                    programId = "prog_strength",
                    programName = "5x5 & Heavy Compound Strength",
                    dayLabel = "Strength A",
                    exercises = listOf(
                        entry("ex_squat", 5, 5, 5, 85.0, 180),
                        entry("ex_bench_press", 5, 5, 5, 70.0, 150),
                        entry("ex_barbell_row", 4, 6, 8, 60.0, 120),
                        entry("ex_farmers_carry", 3, 30, 40, 24.0, 75)
                    )
                ),
                WorkoutPlan(
                    id = "tpl_strength_b",
                    name = "Strength Day B — Deadlift & Press",
                    level = "advanced",
                    targetGoal = "muscle_gain",
                    programId = "prog_strength",
                    programName = "5x5 & Heavy Compound Strength",
                    dayLabel = "Strength B",
                    exercises = listOf(
                        entry("ex_deadlift", 3, 3, 5, 100.0, 180),
                        entry("ex_ohp", 4, 5, 6, 45.0, 150),
                        entry("ex_pullups", 3, 6, 8, 0.0, 120),
                        entry("ex_hanging_leg_raise", 3, 10, 12, 0.0, 60)
                    )
                )
            )
        ),
        WorkoutProgram(
            id = "prog_hypertrophy",
            name = "Hypertrophy Sculpt (Muscle Building)",
            category = "hypertrophy",
            goal = "muscle_gain",
            level = "intermediate",
            daysPerWeek = 4L,
            description = "8–15 rep controlled eccentric training engineered for mechanical tension and metabolic stress.",
            weeklySplit = listOf("Day 1: Upper Hypertrophy", "Day 2: Lower Hypertrophy", "Day 4: Push + Arms", "Day 5: Pull + Glutes"),
            progressionGuidance = "Use 2-second controlled negatives and stay 1–2 reps shy of muscular failure.",
            workouts = listOf(
                WorkoutPlan(
                    id = "tpl_hyp_upper",
                    name = "Upper Hypertrophy Volume",
                    level = "intermediate",
                    targetGoal = "muscle_gain",
                    programId = "prog_hypertrophy",
                    programName = "Hypertrophy Sculpt (Muscle Building)",
                    dayLabel = "Upper Hyp",
                    exercises = listOf(
                        entry("ex_incline_db_press", 4, 8, 12, 20.0, 90),
                        entry("ex_lat_pulldown", 4, 10, 12, 45.0, 75),
                        entry("ex_cable_fly", 3, 12, 15, 12.0, 60),
                        entry("ex_lateral_raise", 4, 12, 16, 8.0, 60),
                        entry("ex_cable_curl", 3, 12, 15, 18.0, 60),
                        entry("ex_oh_tricep_ext", 3, 12, 15, 16.0, 60)
                    )
                )
            )
        ),
        WorkoutProgram(
            id = "prog_beginner",
            name = "Beginner Starter Blueprint",
            category = "beginner",
            goal = "general_fitness",
            level = "beginner",
            daysPerWeek = 3L,
            description = "Joint-friendly machine and dumbbell program designed to master movement mechanics in weeks 1–8.",
            weeklySplit = listOf("Mon: Machine & DB Basics", "Wed: Core & Posture", "Fri: Machine & DB Basics"),
            progressionGuidance = "Prioritize smooth tempo over heavy plates for your first 4 weeks.",
            workouts = listOf(
                WorkoutPlan(
                    id = "tpl_beginner_core",
                    name = "Beginner Full-Body Confidence",
                    level = "beginner",
                    targetGoal = "general_fitness",
                    programId = "prog_beginner",
                    programName = "Beginner Starter Blueprint",
                    dayLabel = "Starter",
                    exercises = listOf(
                        entry("ex_leg_press", 3, 10, 12, 60.0, 90),
                        entry("ex_chest_press_machine", 3, 10, 12, 30.0, 75),
                        entry("ex_seated_cable_row", 3, 10, 12, 30.0, 75),
                        entry("ex_db_shoulder_press", 2, 10, 12, 8.0, 75),
                        entry("ex_plank", 3, 30, 45, 0.0, 45)
                    )
                )
            )
        ),
        WorkoutProgram(
            id = "prog_home",
            name = "Home Training (Bands & Bodyweight)",
            category = "home",
            goal = "weight_loss",
            level = "beginner",
            daysPerWeek = 4L,
            description = "Zero-gym routine utilizing bodyweight leverage, resistance bands, and optional kettlebells.",
            weeklySplit = listOf("Mon: Home Upper & Core", "Tue: Home Lower & Conditioning", "Thu: Home Upper & Core", "Sat: Home Full Body Burn"),
            progressionGuidance = "Shorten rest intervals slightly or add 2 reps per week to progress without heavy iron.",
            workouts = listOf(
                WorkoutPlan(
                    id = "tpl_home_hiit",
                    name = "Home Strength & Conditioning",
                    level = "beginner",
                    targetGoal = "weight_loss",
                    programId = "prog_home",
                    programName = "Home Training (Bands & Bodyweight)",
                    dayLabel = "Home Burn",
                    exercises = listOf(
                        entry("ex_pushups", 3, 10, 15, 0.0, 60),
                        entry("ex_band_row", 3, 12, 16, 0.0, 60),
                        entry("ex_glute_bridge", 3, 15, 20, 0.0, 45),
                        entry("ex_band_ohp", 3, 12, 15, 0.0, 60),
                        entry("ex_burpees", 3, 8, 12, 0.0, 60),
                        entry("ex_plank", 3, 30, 45, 0.0, 45)
                    )
                )
            )
        )
    )

    /**
     * Scales workout sets, rest periods, and target reps based on user age, goal, and activity level
     * so beginners/minors/older athletes receive appropriate volume instead of identical static templates.
     */
    fun personalizedProgramsForUser(profile: UserProfile?): List<WorkoutProgram> {
        val age = (profile?.ageYears ?: 28L).toInt()
        val activity = profile?.activityLevel ?: "moderate"
        val goal = profile?.goal ?: "general_fitness"

        val setDelta = when {
            age < 18 -> -1L
            age >= 55 -> -1L
            activity in listOf("sedentary", "light") -> -1L
            activity == "very_active" && goal == "muscle_gain" -> 1L
            else -> 0L
        }
        val restBonus = when {
            age >= 55 -> 15L
            goal == "weight_loss" -> -15L
            else -> 0L
        }

        return basePrograms.map { program ->
            val scaledWorkouts = program.workouts.map { plan ->
                val scaledExercises = plan.exercises.map { ex ->
                    val minSets = if (age < 18 || activity == "sedentary") 2L else 2L
                    val adjustedSets = (ex.targetSets + setDelta).coerceIn(minSets, 6L)
                    val adjustedRest = (ex.restSeconds + restBonus).coerceIn(30L, 210L)
                    ex.copy(
                        targetSets = adjustedSets,
                        restSeconds = adjustedRest
                    )
                }
                plan.copy(exercises = scaledExercises)
            }
            val personalizationTag = buildString {
                if (setDelta < 0L) append("Volume scaled for recovery/age. ")
                else if (setDelta > 0L) append("High-capacity volume (+1 working set). ")
                append(program.progressionGuidance)
            }
            program.copy(
                workouts = scaledWorkouts,
                progressionGuidance = personalizationTag
            )
        }
    }

    fun starterTemplatesForUser(profile: UserProfile?): List<WorkoutPlan> =
        personalizedProgramsForUser(profile).flatMap { it.workouts }

    val starterTemplates: List<WorkoutPlan> = basePrograms.flatMap { it.workouts }
}
