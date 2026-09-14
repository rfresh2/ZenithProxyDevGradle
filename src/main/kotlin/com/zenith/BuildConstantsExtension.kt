package com.zenith

import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class BuildConstantsExtension {
    @Inject
    public constructor(project: Project) {
        packageGroup.convention(project.provider { project.group.toString() })
        className.convention("BuildConstants")
        fields.convention(emptyMap())
    }

    /** Package of the generated class **/
    public abstract val packageGroup: Property<String>

    /** Generated class name. **/
    public abstract val className: Property<String>

    /** Java field names mapped to string constant values. */
    public abstract val fields: MapProperty<String, String>
}
