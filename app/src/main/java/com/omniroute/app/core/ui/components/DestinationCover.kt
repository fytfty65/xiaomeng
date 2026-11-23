package com.omniroute.app.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.omniroute.app.R

private enum class TravelPhoto(val art: Int, val place: String, val author: String, val source: String, val version: String) {
    Tianchi(R.drawable.destination_tianchi, "天山天池", "Bernard Gagnon", "Tianchi_Lake.jpg", "3.0"),
    Urumqi(R.drawable.destination_urumqi, "乌鲁木齐", "Albertaont", "Urumqi_Skyline_July_2019.jpg", "4.0"),
    Qingdao(R.drawable.destination_qingdao, "青岛栈桥", "K.Y.K.Z.K.", "Zhanqiao_pier_with_Little_Qingdao_Isle.jpg", "4.0"),
}

@Composable
internal fun DestinationCover(
    city: String?,
    visitsTianchi: Boolean,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val photo = when (city?.trim()?.removeSuffix("市")) {
        "乌鲁木齐" -> if (visitsTianchi) TravelPhoto.Tianchi else TravelPhoto.Urumqi
        "天山天池", "阜康" -> TravelPhoto.Tianchi
        "青岛" -> TravelPhoto.Qingdao
        else -> null
    }
    if (photo == null) {
        SeasonArtwork(modifier, cover = true, content = content)
        return
    }
    var showCredit by remember(photo) { mutableStateOf(false) }
    Box(modifier) {
        Image(painterResource(photo.art), photo.place, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        content()
        IconButton(onClick = { showCredit = true }, modifier = Modifier.align(Alignment.TopStart)) {
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.27f)) {
                Icon(Icons.Filled.Info, "风景摄影署名", modifier = Modifier.padding(5.dp).size(15.dp), tint = Color.White)
            }
        }
    }
    if (showCredit) {
        val uri = LocalUriHandler.current
        AlertDialog(onDismissRequest = { showCredit = false }, title = { Text(photo.place) },
            text = { Text("摄影  ${photo.author}\nCC BY-SA ${photo.version} · Wikimedia Commons\n应用内按屏幕比例裁切展示。") },
            confirmButton = { TextButton(onClick = { uri.openUri("https://commons.wikimedia.org/wiki/File:${photo.source}") }) { Text("查看原作") } },
            dismissButton = { TextButton(onClick = { uri.openUri("https://creativecommons.org/licenses/by-sa/${photo.version}/") }) { Text("使用许可") } })
    }
}
