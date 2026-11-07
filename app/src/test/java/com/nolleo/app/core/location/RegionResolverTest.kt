package com.nolleo.app.core.location

import com.nolleo.app.data.ContentParser
import com.nolleo.app.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class RegionResolverTest {

    private val resolver = RegionResolver(
        ContentParser.parseRegions(File("src/main/assets/content/regions.json").readText()),
    )

    private fun at(lat: Double, lng: Double) = resolver.resolve(GeoPoint(lat, lng))

    @Test
    fun metropolitanCities() {
        assertEquals("seoul", at(37.5665, 126.9780))
        assertEquals("busan", at(35.1152, 129.0422))
        assertEquals("daegu", at(35.8714, 128.6014))
        assertEquals("incheon", at(37.4563, 126.7052))
        assertEquals("gwangju", at(35.1595, 126.8526))
        assertEquals("daejeon", at(36.3504, 127.3845))
        assertEquals("ulsan", at(35.5384, 129.3114))
        assertEquals("sejong", at(36.4800, 127.2890))
    }

    @Test
    fun provincesAroundMetropolitanCities() {
        assertEquals("gyeonggi", at(37.4128, 127.0948)) // 성남 소프트웨어드림센터 (서울 박스 안이지만 반경 밖)
        assertEquals("gyeonggi", at(37.2636, 127.0286)) // 수원
        assertEquals("gyeongnam", at(35.2281, 128.6811)) // 창원
        assertEquals("gyeongbuk", at(36.1195, 128.3446)) // 구미
    }

    @Test
    fun overlappingBoxes_pickNearestCenter() {
        assertEquals("chungbuk", at(36.6424, 127.4890)) // 청주 (충남 박스와 겹침)
        assertEquals("chungbuk", at(36.9910, 127.9260)) // 충주
        assertEquals("chungnam", at(36.8151, 127.1139)) // 천안
        assertEquals("gangwon", at(37.3422, 127.9202)) // 원주
        assertEquals("gangwon", at(37.7519, 128.8761)) // 강릉
        assertEquals("jeonbuk", at(35.8242, 127.1480)) // 전주
        assertEquals("jeonnam", at(34.8118, 126.3922)) // 목포
        assertEquals("gyeongbuk", at(36.0190, 129.3435)) // 포항
        assertEquals("jeju", at(33.4996, 126.5312))
    }

    @Test
    fun outsideKorea_isNull() {
        // 에뮬레이터 기본 GPS (미국 마운틴뷰)
        assertNull(at(37.4220, -122.0840))
    }

    @Test
    fun fromAddress_usesMostSpecificKeyword() {
        assertEquals("gyeonggi", resolver.fromAddress("수정구 시흥동"))
        assertEquals("gyeonggi", resolver.fromAddress("경기도 성남시 분당구"))
        assertEquals("seoul", resolver.fromAddress("서울특별시 강남구 남부순환로 2621"))
        assertEquals("busan", resolver.fromAddress("부산광역시 동구 중앙대로 206"))
        assertEquals("chungbuk", resolver.fromAddress("충주시 호암동"))
        assertNull(resolver.fromAddress(""))
        assertNull(resolver.fromAddress("중구 명동"))
    }
}
