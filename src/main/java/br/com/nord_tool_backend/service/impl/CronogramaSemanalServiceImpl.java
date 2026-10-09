package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.dto.CronogramaSemanalDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.form.CronogramaSemanalForm;
import br.com.nord_tool_backend.repository.CronogramaSemanalRepository;
import br.com.nord_tool_backend.service.CronogramaSemanalService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CronogramaSemanalServiceImpl implements CronogramaSemanalService {
    private final Logger log = LogManager.getLogger(CronogramaSemanalServiceImpl.class);

    private final CronogramaSemanalRepository cronogramaSemanalRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CronogramaSemanalDto salvarCronogramaSemanal(CronogramaSemanalForm cronogramaSemanalForm) {
        log.info("Iniciando método para salvar um Cronograma Semanal");
        CronogramaSemanal cronogramaSemanal = cronogramaSemanalForm.converterToDomain();
        validarOrigemCronogramaSemanal(cronogramaSemanal);
        CronogramaSemanalDto cronogramaSemanalDto = cronogramaSemanalRepository.salvarCronogramaSemanal(cronogramaSemanal);
        log.info("Finalizando método que salva um Cronograma Semanal");
        return cronogramaSemanalDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CronogramaSemanalDto alterarCronogramaSemanal(CronogramaSemanalForm cronogramaSemanalForm) {
        log.info("Iniciando método para alterar um Cronograma Semanal");
        CronogramaSemanal cronogramaSemanal = cronogramaSemanalForm.converterToDomain();
        validarOrigemCronogramaSemanal(cronogramaSemanal);
        CronogramaSemanalDto cronogramaSemanalDto = cronogramaSemanalRepository.alterarCronogramaSemanal(cronogramaSemanal);
        log.info("Finalizando método que alterar um Cronograma Semanal");
        return cronogramaSemanalDto;
    }

    private void validarOrigemCronogramaSemanal(CronogramaSemanal cronogramaSemanal) {
        boolean fixo = Boolean.TRUE.equals(cronogramaSemanal.getInCronogramaFixo());
        if (fixo && cronogramaSemanal.getIdDiaSemana() == null) {
            throw new ValidacaoException(NordHttpEnum.HTTP_400,
                    "Atividades fixas precisam de um dia da semana.",
                    "idDiaSemana ausente para item fixo");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletarCronogramaSemanal(Long id) {
        log.info("Iniciando método para deletar um Cronograma Semanal");
        this.cronogramaSemanalRepository.deletarCronogramaSemanal(id);
        log.info("Finalizando método que deleta um Cronograma Semanal");
    }

    @Override
    public CronogramaSemanalDto buscarPorIdCronogramaSemanal(Long id) {
        log.info("Iniciando método para buscar um Cronograma Semanal por id");
        CronogramaSemanal cronogramaSemanal = cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(id);
        CronogramaSemanalDto cronogramaSemanalDto = CronogramaSemanalDto.converterToDto(cronogramaSemanal);
        log.info("Finalizando método que busca um Cronograma Semanal por id");
        return cronogramaSemanalDto;
    }

    @Override
    public List<CronogramaSemanalDto> listarCronogramaSemanal() {
        log.info("Iniciando método para listar Cronograma Semanal");
        List<CronogramaSemanal> lsCronogramaSemanal = cronogramaSemanalRepository.listarCronogramaSemanal();
        List<CronogramaSemanalDto> lsCronogramaSemanalDto = lsCronogramaSemanal.stream().map(CronogramaSemanalDto::converterToDto).collect(Collectors.toList());
        log.info("Finalizando método que lista Cronograma Semanal");
        return lsCronogramaSemanalDto;
    }
}
