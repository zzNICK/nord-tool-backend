package br.com.nord_tool_backend.handler;

import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.domain.ApartamentoVistoria;
import br.com.nord_tool_backend.domain.enums.DiaSemanaEnum;
import br.com.nord_tool_backend.domain.enums.StatusVistoriaEnum;
import br.com.nord_tool_backend.repository.ApartamentoVistoriaRepository;
import br.com.nord_tool_backend.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class XlsxExtractorHandlerApartamento extends StringUtils {
    private static final String ERRO_LINHA_PLANILHA = "Erro ao processar dados da planilha. Linha erro:";
    private static final String ERRO_MONTAGEM_PLANILHA_TO_OBJ = "Erro ao montar dados da planilha para o objeto";

    // Tornar protected para a subclasse enxergar sem criar um atributo duplicado
    protected final ApartamentoVistoriaRepository apartamentoVistoriaRepository;

    public void init(MultipartFile arquivo) throws Exception {

        List<ApartamentoVistoria> lsApartamentoVistoria = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(arquivo.getInputStream())) {
            log.info("Iniciando extração e leitura dos dados da planilha");
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                log.info("Finalizando extração e leitura dos dados da planilha");
                try {
                    log.info("Iniciando montagem dos dados da planilha para o objeto");
                    lsApartamentoVistoria.add(buildRowApartamento(row));
                    log.info("Finalizando montagem dos dados da planilha para o objeto");
                } catch (Exception ex) {
                    log.error(ExceptionUtils.getMessage(ex));
                    throw new EntradaInvalidaException(ERRO_LINHA_PLANILHA + (i + 1), ex);
                }
            }
        }
        log.info("Iniciando salvamento do objeto na base de dados");
        apartamentoVistoriaRepository.salvarEmLote(lsApartamentoVistoria);
        log.info("Finzalizando salvamento do objeto na base de dados");
    }

    private ApartamentoVistoria buildRowApartamento(Row row) {
        try{
            Integer idDiaSemana = DiaSemanaEnum.getDiaSemana(getCellString(row,0));
            LocalDate dtApartamentoVigente = parseDate(getCellString(row, 1));
            String nmHorarioVistoria = parseHours(getCellString(row, 2));
            String nmApartamentoVistoria = getCellString(row, 3);
            boolean inMarcarRevistoria = parseBoolean(row.getCell(4));
            LocalDate dtRevistoriaVigente = parseDate(getCellString(row, 5));
            Integer idStatusVistoria = StatusVistoriaEnum.getStatusVistoria(getCellString(row, 6));
            String txObservacaoRevistoria = getCellString(row, 7);

            return ApartamentoVistoria.builder()
                    .nmApartamentoVistoria(nmApartamentoVistoria)
                    .idDiaSemana(idDiaSemana)
                    .dtApartamentoVigente(dtApartamentoVigente)
                    .nmHorarioVistoria(nmHorarioVistoria)
                    .idStatusVistoria(idStatusVistoria)
                    .inMarcarRevistoria(inMarcarRevistoria)
                    .txObservacaoRevistoria(txObservacaoRevistoria)
                    .dtRevistoriaVigente(dtRevistoriaVigente)
                    .build();
        } catch (Exception ex) {
            log.error(ExceptionUtils.getMessage(ex));
            throw new EntradaInvalidaException(ERRO_MONTAGEM_PLANILHA_TO_OBJ, ex);
        }
    }
}
