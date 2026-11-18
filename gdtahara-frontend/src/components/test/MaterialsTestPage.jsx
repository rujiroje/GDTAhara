// =================================================================
// ไฟล์ทดสอบ Materials พร้อม Mock Data สำหรับ Debug CM Operator
// =================================================================
import React, { useState, useEffect } from 'react';

// Mock data สำหรับทดสอบ
const mockMaterials = [
    { id: 1, materialCode: 'PE001', materialName: 'PE ไวร์จิน ชนิด A', materialType: 'VIRGIN', unit: 'KG' },
    { id: 2, materialCode: 'PE002', materialName: 'PE ไวร์จิน ชนิด B', materialType: 'VIRGIN', unit: 'KG' },
    { id: 3, materialCode: 'AD001', materialName: 'ADMER สำหรับการยึดติด', materialType: 'ADMER', unit: 'KG' },
    { id: 4, materialCode: 'AD002', materialName: 'ADMER ชนิดพิเศษ', materialType: 'ADMER', unit: 'KG' },
    { id: 5, materialCode: 'EV001', materialName: 'EVOH สำหรับกันออกซิเจน', materialType: 'EVOH', unit: 'KG' },
    { id: 6, materialCode: 'EV002', materialName: 'EVOH ชั้นบาง', materialType: 'EVOH', unit: 'KG' },
    { id: 7, materialCode: 'MX001', materialName: 'วัสดุผสม ทั่วไป', materialType: 'MIX', unit: 'KG' },
    { id: 8, materialCode: 'MX002', materialName: 'วัสดุผสม พิเศษ', materialType: 'MIX', unit: 'KG' },
    { id: 9, materialCode: 'PE003', materialName: 'PE กรีน', materialType: 'Virgin', unit: 'KG' }, // ต่างกรณี
    { id: 10, materialCode: 'AD003', materialName: 'Admer สีฟ้า', materialType: 'admer', unit: 'KG' }, // ต่างกรณี
];

const _mockReports = [
    { id: 1, orderNumber: 'ORD-2025-001', machineName: 'Machine A', productName: 'Product X', startDate: '2025-09-19', endDate: '2025-09-20' },
    { id: 2, orderNumber: 'ORD-2025-002', machineName: 'Machine B', productName: 'Product Y', startDate: '2025-09-19', endDate: '2025-09-20' },
];

const MaterialsTestPage = () => {
    const [selectedMaterialType, setSelectedMaterialType] = useState('');
    const [debugInfo, setDebugInfo] = useState('');

    const materials = mockMaterials;

    // สร้าง materialTypes จากข้อมูลจริงในฐานข้อมูล
    const materialTypes = materials && Array.isArray(materials) 
        ? [...new Set(materials.map(m => m.materialType).filter(type => type && type.trim() !== ''))]
        : ["VIRGIN", "ADMER", "EVOH", "MIX"];
        
    const filteredMaterials = (materials && Array.isArray(materials)) 
        ? materials.filter(m => m.materialType && m.materialType.trim().toLowerCase() === selectedMaterialType.toLowerCase())
        : [];

    useEffect(() => {
        let debug = '';
        debug += `=== Materials Debug Info ===\n`;
        debug += `Total Materials: ${materials ? materials.length : 0}\n`;
        debug += `Available Material Types: ${JSON.stringify(materialTypes)}\n`;
        debug += `Selected Material Type: "${selectedMaterialType}"\n`;
        debug += `Filtered Materials Count: ${filteredMaterials.length}\n\n`;
        
        if (materials && materials.length > 0) {
            debug += `First 3 materials sample:\n${JSON.stringify(materials.slice(0, 3), null, 2)}\n\n`;
            const uniqueTypes = [...new Set(materials.map(m => m.materialType))];
            debug += `All unique material types found: ${JSON.stringify(uniqueTypes)}\n\n`;
            
            uniqueTypes.forEach(type => {
                const count = materials.filter(m => m.materialType === type).length;
                debug += `Type "${type}": ${count} materials\n`;
            });
        }
        
        if (selectedMaterialType) {
            debug += `\nLooking for materials with type: "${selectedMaterialType}"\n`;
            debug += `Filtered materials found: ${filteredMaterials.length}\n`;
            if (filteredMaterials.length > 0) {
                debug += `Sample filtered materials:\n${JSON.stringify(filteredMaterials.slice(0, 2), null, 2)}\n`;
            } else {
                debug += `No exact matches found. Checking for case-insensitive matches...\n`;
                const caseInsensitiveMatches = materials.filter(m => 
                    m.materialType && m.materialType.toLowerCase() === selectedMaterialType.toLowerCase()
                );
                debug += `Case-insensitive matches: ${caseInsensitiveMatches.length}\n`;
                debug += `Sample: ${JSON.stringify(caseInsensitiveMatches, null, 2)}\n`;
            }
        }

        setDebugInfo(debug);
    }, [selectedMaterialType, materials, materialTypes, filteredMaterials]);

    return (
        <div style={{ padding: '20px', fontFamily: 'monospace' }}>
            <h1>Materials Filtering Test Page</h1>
            
            <div style={{ marginBottom: '20px' }}>
                <label>เลือกชนิดวัตถุดิบ: </label>
                <select 
                    value={selectedMaterialType} 
                    onChange={(e) => setSelectedMaterialType(e.target.value)}
                    style={{ marginLeft: '10px', padding: '5px' }}
                >
                    <option value="">-- เลือกชนิด --</option>
                    {materialTypes.map(type => (
                        <option key={type} value={type}>{type}</option>
                    ))}
                </select>
            </div>

            <div style={{ marginBottom: '20px' }}>
                <label>เลือกวัตถุดิบ: </label>
                <select disabled={!selectedMaterialType} style={{ marginLeft: '10px', padding: '5px', width: '300px' }}>
                    <option value="">-- เลือกวัตถุดิบ --</option>
                    {filteredMaterials.map(m => (
                        <option key={m.id} value={m.id}>
                            {m.materialName} ({m.materialCode})
                        </option>
                    ))}
                </select>
                <span style={{ marginLeft: '10px', color: filteredMaterials.length === 0 ? 'red' : 'green' }}>
                    {filteredMaterials.length} รายการ
                </span>
            </div>

            <div style={{ backgroundColor: '#f5f5f5', padding: '15px', borderRadius: '5px' }}>
                <h3>Debug Information:</h3>
                <pre style={{ fontSize: '12px', whiteSpace: 'pre-wrap' }}>{debugInfo}</pre>
            </div>
        </div>
    );
};

export default MaterialsTestPage;