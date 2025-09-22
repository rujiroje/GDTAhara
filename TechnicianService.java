import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TechnicianService {

    // ...existing code...

    @Transactional
    public ParameterRecord createParameterRecord(Long reportId, ParameterRecordDto parameterDto) {
        // Validate report existence
        machineReportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));

        ParameterRecord newRecord = new ParameterRecord();
        newRecord.setMachineReportId(reportId);
        newRecord.setCreatedAt(LocalDateTime.now());
        mapDtoToEntity(parameterDto, newRecord);

        return parameterRecordRepository.save(newRecord);
    }

    @Transactional
    public ParameterRecord updateParameterRecord(Long recordId, ParameterRecordDto parameterDto) {
        ParameterRecord existingRecord = parameterRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Parameter Record not found with id: " + recordId));

        mapDtoToEntity(parameterDto, existingRecord);
        existingRecord.setUpdatedAt(LocalDateTime.now());

        return parameterRecordRepository.save(existingRecord);
    }

    private void mapDtoToEntity(ParameterRecordDto dto, ParameterRecord entity) {
        entity.setRecordType(dto.getRecordType());

        // Map Extruder Screw
        mapExtruderScrew(dto.getExtruderScrew(), entity);

        // Map Temperature
        mapTemperature(dto.getTemperature(), entity);
    }

    private void mapExtruderScrew(ParameterRecordDto.ExtruderScrewDto extruder, ParameterRecord entity) {
        entity.setMainScrewRpm(toBigDecimal(extruder.getMain().getScrewRpm()));
        entity.setMainLimitPress(toBigDecimal(extruder.getMain().getLimitPress()));
        entity.setMainCurrent(toBigDecimal(extruder.getMain().getCurrent()));
        entity.setMainResinTemp(toBigDecimal(extruder.getMain().getResinTemp()));
        entity.setAdmerScrewRpm(toBigDecimal(extruder.getAdmer().getScrewRpm()));
        entity.setAdmerLimitPress(toBigDecimal(extruder.getAdmer().getLimitPress()));
        entity.setAdmerCurrent(toBigDecimal(extruder.getAdmer().getCurrent()));
        entity.setAdmerResinTemp(toBigDecimal(extruder.getAdmer().getResinTemp()));
        entity.setEvohScrewRpm(toBigDecimal(extruder.getEvoh().getScrewRpm()));
        entity.setEvohLimitPress(toBigDecimal(extruder.getEvoh().getLimitPress()));
        entity.setEvohCurrent(toBigDecimal(extruder.getEvoh().getCurrent()));
        entity.setEvohResinTemp(toBigDecimal(extruder.getEvoh().getResinTemp()));
        entity.setVirginScrewRpm(toBigDecimal(extruder.getVirgin().getScrewRpm()));
        entity.setVirginLimitPress(toBigDecimal(extruder.getVirgin().getLimitPress()));
        entity.setVirginCurrent(toBigDecimal(extruder.getVirgin().getCurrent()));
        entity.setVirginResinTemp(toBigDecimal(extruder.getVirgin().getResinTemp()));
    }

    private void mapTemperature(ParameterRecordDto.TemperatureDto temp, ParameterRecord entity) {
        entity.setTempMainFb(toBigDecimal(temp.getMain().getFb()));
        entity.setTempMainC1(toBigDecimal(temp.getMain().getC1()));
        entity.setTempMainC2(toBigDecimal(temp.getMain().getC2()));
        entity.setTempMainC3(toBigDecimal(temp.getMain().getC3()));
        entity.setTempMainA1(toBigDecimal(temp.getMain().getA1()));
        entity.setTempMainA2(toBigDecimal(temp.getMain().getA2()));
        entity.setTempMainA3(toBigDecimal(temp.getMain().getA3()));
        entity.setTempMainA4(toBigDecimal(temp.getMain().getA4()));
        entity.setTempAdmerFb(toBigDecimal(temp.getAdmer().getFb()));
        entity.setTempAdmerC1(toBigDecimal(temp.getAdmer().getC1()));
        entity.setTempAdmerC2(toBigDecimal(temp.getAdmer().getC2()));
        entity.setTempAdmerC3(toBigDecimal(temp.getAdmer().getC3()));
        entity.setTempAdmerA1(toBigDecimal(temp.getAdmer().getA1()));
        entity.setTempEvohFb(toBigDecimal(temp.getEvoh().getFb()));
        entity.setTempEvohC1(toBigDecimal(temp.getEvoh().getC1()));
        entity.setTempEvohC2(toBigDecimal(temp.getEvoh().getC2()));
        entity.setTempEvohC3(toBigDecimal(temp.getEvoh().getC3()));
        entity.setTempEvohA1(toBigDecimal(temp.getEvoh().getA1()));
        entity.setTempVirginFb(toBigDecimal(temp.getVirgin().getFb()));
        entity.setTempVirginC1(toBigDecimal(temp.getVirgin().getC1()));
        entity.setTempVirginC2(toBigDecimal(temp.getVirgin().getC2()));
        entity.setTempVirginC3(toBigDecimal(temp.getVirgin().getC3()));
        entity.setTempVirginA1(toBigDecimal(temp.getVirgin().getA1()));
        entity.setTempVirginA2(toBigDecimal(temp.getVirgin().getA2()));
        entity.setTempVirginA3(toBigDecimal(temp.getVirgin().getA3()));
        entity.setTempVirginA4(toBigDecimal(temp.getVirgin().getA4()));
        entity.setTempHeadD1_1(toBigDecimal(temp.getHead().getD1_1()));
        entity.setTempHeadD2_1(toBigDecimal(temp.getHead().getD2_1()));
        entity.setTempHeadD3_1(toBigDecimal(temp.getHead().getD3_1()));
        entity.setTempHeadD4_1(toBigDecimal(temp.getHead().getD4_1()));
        entity.setTempHeadD1_2(toBigDecimal(temp.getHead().getD1_2()));
        entity.setTempHeadD2_2(toBigDecimal(temp.getHead().getD2_2()));
        entity.setTempHeadD3_2(toBigDecimal(temp.getHead().getD3_2()));
        entity.setTempHeadD4_2(toBigDecimal(temp.getHead().getD4_2()));
    }

    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }
}
