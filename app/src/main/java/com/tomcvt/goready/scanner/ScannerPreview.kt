package com.tomcvt.goready.scanner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tomcvt.goready.LocalOverlayHost
import com.tomcvt.goready.LocalScanSetRepositoryProvider
import com.tomcvt.goready.data.ScanSetEntity
import com.tomcvt.goready.repository.ScanSetRepository
import com.tomcvt.goready.ui.overlay.OverlayHost
import com.tomcvt.goready.ui.theme.GoReadyTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// ─────────────────────────────────────────────────────────────────────────────
// Mock ScanSetRepository for preview/testing
// ─────────────────────────────────────────────────────────────────────────────

private class MockScanSetRepository : ScanSetRepository {
    private val fakeSets = listOf(
        ScanSetEntity(id = 1, name = "Morning routine", encodedCodes = "ABC123:Scan locker,DEF456:", size = 2),
        ScanSetEntity(id = 2, name = "Gym set", encodedCodes = "GYM001:Entrance", size = 1),
        ScanSetEntity(id = 3, name = "Office access", encodedCodes = "DOOR1:Front door,DOOR2:Server room,DOOR3:Break room", size = 3),
    )

    override fun getScanSets(): Flow<List<ScanSetEntity>> = flowOf(fakeSets)

    override suspend fun insertScanSet(scanSet: ScanSetEntity): Long = 99L

    override suspend fun deleteScanSet(scanSet: ScanSetEntity) = Unit

    override suspend fun updateScanSet(scanSet: ScanSetEntity) = Unit

    override suspend fun getScanSetById(id: Long): ScanSetEntity =
        fakeSets.firstOrNull { it.id == id }
            ?: ScanSetEntity(id = id, name = "Unknown", encodedCodes = "", size = 0)

    override fun decodeScanCodes(encodedCodes: String): List<ScanCode> =
        decodeScanCodesR(encodedCodes)

    override fun encodeScanCodes(codes: List<ScanCode>): String =
        encodeScanCodesR(codes)
}

// ─────────────────────────────────────────────────────────────────────────────
// MultiBarcodesSaverViewPreview
//
// The preview is possible because:
//  • ScanSetRepository is provided via a mock (no DB needed)
//  • OverlayHost is created locally and its entries are rendered in the same Box
//  • The only non-previewable part is the camera screen (SaveBarcodeScreen /
//    BarcodeScannerView) — tapping the "+" to scan will open it via overlayHost,
//    but the camera won't initialise in a preview frame. All other UI —
//    the toolbar row, code list cards, Load/Save-As/Save buttons, and both
//    modals — render correctly.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MultiBarcodesSaverLoadedPreview() {
    val mockSet = ScanSetEntity(
        id = 7,
        name = "Airport bag check",
        encodedCodes = "PASS001:Boarding pass,BAG002:Checked baggage,LOUNGE03:Lounge access",
        size = 3
    )
    val mockCodes = listOf(
        ScanCode(barcode = "PASS001", tip = "Boarding pass"),
        ScanCode(barcode = "BAG002", tip = "Checked baggage"),
        ScanCode(barcode = "LOUNGE03", tip = "Lounge access")
    )
    val mockRepository = remember { MockScanSetRepository() }
    val overlayHost = remember { OverlayHost() }

    GoReadyTheme {
        CompositionLocalProvider(
            LocalScanSetRepositoryProvider provides mockRepository,
            LocalOverlayHost provides overlayHost
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MultiBarcodesSaverView(
                    //initialCodes = mockCodes,
                    loadedSet = mockSet,
                    onDismiss = {},
                    onAccept = {}
                )
                overlayHost.entries.forEach { entry ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        entry.content()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MultiBarcodesSaverViewPreview() {
    val mockRepository = remember { MockScanSetRepository() }
    val overlayHost = remember { OverlayHost() }

    GoReadyTheme {
        CompositionLocalProvider(
            LocalScanSetRepositoryProvider provides mockRepository,
            LocalOverlayHost provides overlayHost
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MultiBarcodesSaverView(
                    initialCodes = listOf(
                        ScanCode(barcode = "TICKET123", tip = "Scan train ticket"),
                        ScanCode(barcode = "LOCKER07", tip = "")
                    ),
                    onDismiss = {},
                    onAccept = {}
                )
                // Render overlay entries so modals show up in the preview
                overlayHost.entries.forEach { entry ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        entry.content()
                    }
                }
            }
        }
    }
}
