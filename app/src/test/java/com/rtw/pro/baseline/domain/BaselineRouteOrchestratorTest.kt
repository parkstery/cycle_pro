package com.rtw.pro.baseline.domain

import com.rtw.pro.baseline.data.common.HttpResult
import com.rtw.pro.baseline.data.elevation.OpenElevationApi
import com.rtw.pro.baseline.data.elevation.OpenElevationService
import com.rtw.pro.baseline.data.routing.OsrmRouteDto
import com.rtw.pro.baseline.data.routing.OsrmRoutingApi
import com.rtw.pro.baseline.data.routing.OsrmRoutingService
import com.rtw.pro.baseline.data.routing.RouteMode
import com.rtw.pro.baseline.domain.geo.LatLng
import kotlin.test.Test
import kotlin.test.assertTrue

class BaselineRouteOrchestratorTest {
    @Test
    fun build_returnsBundle_whenRoutingAndElevationSucceed() {
        val routingApi = object : OsrmRoutingApi {
            override fun fetchRoute(mode: RouteMode, waypoints: List<LatLng>): HttpResult<OsrmRouteDto> {
                return HttpResult.Success(
                    OsrmRouteDto(
                        encodedPolyline = "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
                        distanceMeters = 1200.0,
                        durationSeconds = 240.0
                    )
                )
            }
        }
        val elevationApi = object : OpenElevationApi {
            override fun fetchElevations(samples: List<LatLng>): HttpResult<List<Double?>> {
                return HttpResult.Success(List(samples.size) { idx -> 100.0 + idx })
            }
        }

        val orchestrator = BaselineRouteOrchestrator(
            routingService = OsrmRoutingService(routingApi),
            elevationService = OpenElevationService(elevationApi)
        )

        val result = orchestrator.build(
            mode = RouteMode.CYCLING,
            waypoints = listOf(
                LatLng(37.0, 127.0),
                LatLng(37.01, 127.01)
            )
        )

        assertTrue(result is HttpResult.Success)
    }
}
