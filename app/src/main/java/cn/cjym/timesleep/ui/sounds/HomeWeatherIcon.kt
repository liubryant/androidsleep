package cn.cjym.timesleep.ui.sounds

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.cos
import kotlin.math.sin

// WMO weather codes, matching the iOS HomeWeather appearance groups.
@Composable internal fun HomeWeatherIcon(code:Int?,day:Boolean,modifier:Modifier=Modifier) {
    val partly = code == null || code in listOf(1,2)
    val snow = code in listOf(71,73,75,77,85,86)
    val hail = code in listOf(96,99)
    val fog = code in listOf(45,48)
    val sleet = code in listOf(56,57,66,67)
    val rain = code in listOf(51,53,55,61,63,65,80,81) || sleet
    val thunder = code == 95
    val tint = when {
        code==0 -> Color(0xFFF5A824)
        partly -> Color(0xFFFF9500)
        snow -> Color(0xFF33B3C7)
        hail -> Color(0xFF57A3F0)
        thunder -> Color(0xFF7A85E0)
        rain -> Color(0xFF2E87E6)
        else -> Color(0xFF8E8E93)
    }
    Canvas(modifier) {
        scale(size.width/24f,size.height/24f,pivot=Offset.Zero) {
            fun line(x:Float,y:Float,x2:Float,y2:Float,width:Float=1.6f) =
                drawLine(tint,Offset(x,y),Offset(x2,y2),width,StrokeCap.Round)
            if(code==0 || partly) {
                val cx=if(partly) 16f else 12f
                val cy=if(partly) 7f else 12f
                if(day) {
                    drawCircle(tint,if(partly) 3.5f else 5f,Offset(cx,cy))
                    repeat(8) {i->
                        val a=i*Math.PI/4
                        val r=if(partly) 5f else 7.5f
                        line(cx+cos(a).toFloat()*r,cy+sin(a).toFloat()*r,cx+cos(a).toFloat()*(r+1.5f),cy+sin(a).toFloat()*(r+1.5f),1.4f)
                    }
                } else {
                    val moon=Path().apply {
                        moveTo(cx+2f,cy-5f)
                        cubicTo(cx-7f,cy-5f,cx-7f,cy+6f,cx+2f,cy+6f)
                        cubicTo(cx-2f,cy+3f,cx-2f,cy-2f,cx+2f,cy-5f)
                        close()
                    }
                    drawPath(moon,tint)
                    drawCircle(tint,1f,Offset(cx+5f,cy-2f))
                }
            }
            if(code!=0) {
                val cloud=Path().apply {
                    moveTo(5f,16f)
                    cubicTo(0f,16f,0f,9f,5f,9f)
                    cubicTo(6f,3f,14f,3f,16f,9f)
                    cubicTo(23f,7f,25f,16f,19f,16f)
                    close()
                }
                drawPath(cloud,tint)
                when {
                    fog -> {line(3f,19f,21f,19f);line(6f,22f,18f,22f)}
                    hail -> repeat(3) {drawCircle(tint,1.5f,Offset(6f+it*6f,20f))}
                    snow -> repeat(3) {i->
                        val x=5f+i*7f
                        line(x,18f,x,23f,1.1f);line(x-2f,19f,x+2f,22f,1.1f);line(x+2f,19f,x-2f,22f,1.1f)
                    }
                    thunder -> {
                        drawPath(Path().apply {moveTo(12f,16f);lineTo(8f,21f);lineTo(12f,21f);lineTo(10f,24f);lineTo(17f,18f);lineTo(13f,18f);close()},tint)
                        line(5f,18f,4f,21f);line(20f,18f,19f,21f)
                    }
                    rain -> repeat(3) {i->
                        val x=6f+i*6f
                        if(sleet && i==1) drawCircle(tint,1.4f,Offset(x,21f))
                        else line(x,18f,x-1.5f,if(code in listOf(65,82)) 23f else 21f,if(code in listOf(51,53,55)) 1.1f else 1.8f)
                    }
                }
            }
        }
    }
}
