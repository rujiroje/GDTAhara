package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.SubLot;
import com.gdtahara.gdtaharabackend.print.LabelPrinterClient;
import com.gdtahara.gdtaharabackend.util.ZplLabelBuilder;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelPrintServiceTest {

    @Mock SubLotService      subLotService;
    @Mock ZplLabelBuilder    zplLabelBuilder;
    @Mock LabelPrinterClient printerClient;

    @InjectMocks LabelPrintService service;

    private SubLot stub() {
        SubLot s = new SubLot();
        s.setId(1L);
        s.setSubLotNumber("PL-RBL101-20251001-D-B0001");
        s.setBoxQuantity(600);
        return s;
    }

    // ── preview ──────────────────────────────────────────────────────

    @Test
    void preview_returnsZpl() {
        SubLot s = stub();
        when(subLotService.getById(1L)).thenReturn(s);
        when(zplLabelBuilder.buildSubLotLabel(s)).thenReturn("^XA MOCK ^XZ");

        String result = service.previewSubLotLabel(1L);

        assertThat(result).startsWith("^XA");
    }

    @Test
    void preview_notFound_throws() {
        when(subLotService.getById(99L))
                .thenThrow(new EntityNotFoundException("SubLot not found: 99"));

        assertThatThrownBy(() -> service.previewSubLotLabel(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── print ─────────────────────────────────────────────────────────

    @Test
    void print_callsClientAndMarksLabeled() {
        SubLot s = stub();
        when(subLotService.getById(1L)).thenReturn(s);
        when(zplLabelBuilder.buildSubLotLabel(s)).thenReturn("^XA MOCK ^XZ");

        service.printSubLotLabel(1L, "192.168.1.100:9100", "admin");

        verify(printerClient).print(eq("^XA MOCK ^XZ"), eq("192.168.1.100:9100"));
        verify(subLotService).markAsLabeled(eq(1L), any(), eq("admin"));
    }
}
