package com.austinhodak.thehideout.features.map.models

import android.os.Parcel
import android.os.Parcelable
import com.austinhodak.thehideout.local.GeoPoint
import com.austinhodak.thehideout.local.LocalStore
import java.io.Closeable
import java.util.UUID

data class CustomMarker(
    val id: String? = null,
    var title: String? = null,
    var description: String? = null,
    val map: String? = null,
    val coordinates: GeoPoint? = null,
    var icon: String? = null
) : Parcelable {

    fun longitude() = coordinates?.longitude
    fun latitude() = coordinates?.latitude

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()?.let {
            val lat = it.split(";")[0].toDouble()
            val lon = it.split(";")[1].toDouble()
            GeoPoint(lat, lon)
        },
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(map)
        parcel.writeString("${coordinates?.latitude};${coordinates?.longitude}")
        parcel.writeString(icon)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CustomMarker> {
        override fun createFromParcel(parcel: Parcel): CustomMarker {
            return CustomMarker(parcel)
        }

        override fun newArray(size: Int): Array<CustomMarker?> {
            return arrayOfNulls(size)
        }
    }

}

/** Markers the user placed on the maps, stored under `markers/<id>` in [LocalStore]. */
object CustomMarkers {
    /** Calls [onChange] with the markers of [map] (lowercase map name), now and after each change. */
    fun listen(
        map: String?,
        onChange: (List<CustomMarker>) -> Unit,
    ): Closeable =
        LocalStore.listen("markers") { node ->
            onChange(
                (node as? Map<*, *>)
                    ?.mapNotNull { (id, value) ->
                        LocalStore.convert(value, CustomMarker::class.java)?.copy(id = id.toString())
                    }?.filter { it.map == map }
                    .orEmpty(),
            )
        }

    fun save(marker: CustomMarker) = LocalStore.set("markers/${marker.id ?: UUID.randomUUID()}", marker)

    fun delete(id: String) = LocalStore.remove("markers/$id")
}
