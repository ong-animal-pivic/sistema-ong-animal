package com.umc.sistemaonganimal.api.exceptionhandler;

import com.umc.sistemaonganimal.domain.exception.DomainException;
import com.umc.sistemaonganimal.domain.exception.EntityExistsException;
import com.umc.sistemaonganimal.domain.exception.EntityInUseException;
import com.umc.sistemaonganimal.domain.exception.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUncaughtException(Exception ex) {
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Ocorreu um erro interno inesperado no sistema. Tente novamente e se o problema persistir, entre em contato com o administrador do sistema.", 
                "Erro interno do sistema",
                "https://pivic-ong/erro-interno");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFoundException(EntityNotFoundException ex) {
        return createProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), "Entidade não encontrada",
                "https://pivic-ong/entidade-nao-encontrada");
    }

    @ExceptionHandler(EntityInUseException.class)
    public ProblemDetail handleEntityInUseException(EntityInUseException ex) {
        return createProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), "Entidade em uso",
                "https://pivic-ong/entidade-em-uso");
    }

    @ExceptionHandler(EntityExistsException.class)
    public ProblemDetail handleEntityExistsException(EntityExistsException ex) {
        return createProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), "Entidade já existente",
                "https://pivic-ong/entidade-existente");
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "Violação de regra de negócio",
                "https://pivic-ong/erro-domain");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        return createProblemDetail(HttpStatus.CONFLICT,
                "Os dados informados violam uma regra de integridade do sistema. Verifique os valores e tente novamente.",
                "Violação de integridade de dados",
                "https://pivic-ong/erro-integridade");
    }

    @SuppressWarnings("null")
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problemDetail = createProblemDetail(status,
                "O corpo da requisição é inválido. Verifique a sintaxe e os formatos dos campos (datas no formato yyyy-MM-dd).",
                "Mensagem incompreensível",
                "https://pivic-ong/mensagem-incompreensivel");

        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @SuppressWarnings("null")
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = "Verifique se os campos estão preenchidos corretamente";

        ProblemDetail problemDetail = createProblemDetail(status, detail, "Violação de restrição",
                "https://pivic-ong/erro-restricao");

        Map<String, String> fields = ex.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage()));

        problemDetail.setProperty("detalhes", fields);

        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @SuppressWarnings("null")
    private ProblemDetail createProblemDetail(HttpStatusCode status, String detail, String title, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(type));

        return problem;
    }
}
