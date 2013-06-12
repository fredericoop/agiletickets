package br.com.caelum.agiletickets.controllers;

import static br.com.caelum.vraptor.view.Results.status;

import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import br.com.caelum.agiletickets.domain.Agenda;
import br.com.caelum.agiletickets.domain.DiretorioDeEstabelecimentos;
import br.com.caelum.agiletickets.models.Espetaculo;
import br.com.caelum.agiletickets.models.Periodicidade;
import br.com.caelum.agiletickets.models.Sessao;
import br.com.caelum.vraptor.Get;
import br.com.caelum.vraptor.Path;
import br.com.caelum.vraptor.Post;
import br.com.caelum.vraptor.Resource;
import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.Validator;
import br.com.caelum.vraptor.validator.ValidationMessage;

import com.google.common.base.Strings;

@Resource
public class EspetaculosController {

	private final Agenda agenda;
	private Validator validator;
	private Result result;
	private final String msgEspetaculoEmBranco 	= "Nome do espetáculo nao pode estar em branco";
	private final String msgDescricaoEmBranco	= "Descricao do espetaculo nao pode estar em branco";
	private final String msgLugarEmBranco		= "Voce deve escolher um lugar ou mais";
	private final String msgSemIngresso			= "Nao existem ingressos disponiveis";
	private final String msgIngressoReservado	= "Sessao reservada com sucesso";

	private final DiretorioDeEstabelecimentos estabelecimentos;

	public EspetaculosController(Agenda agenda, DiretorioDeEstabelecimentos estabelecimentos, Validator validator, Result result) {
		this.agenda = agenda;
		this.estabelecimentos = estabelecimentos;
		this.validator = validator;
		this.result = result;
	}

	@Get @Path("/espetaculos")
	public List<Espetaculo> lista() {
		// inclui a lista de estabelecimentos
		result.include("estabelecimentos", estabelecimentos.todos());
		return agenda.espetaculos();
	}

	@Post @Path("/espetaculos")
	public void adiciona(Espetaculo espetaculo) {
		// aqui eh onde fazemos as varias validacoes
		// se nao tiver nome, avisa o usuario
		// se nao tiver descricao, avisa o usuario
		this.validaAdicao(espetaculo);

		agenda.cadastra(espetaculo);
		result.redirectTo(this).lista();
	}

	private void validaAdicao(Espetaculo espetaculo) {
		
		if (Strings.isNullOrEmpty(espetaculo.getNome())) {
			validator.add(new ValidationMessage(msgEspetaculoEmBranco, ""));
		}
		if (Strings.isNullOrEmpty(espetaculo.getDescricao())) {
			validator.add(new ValidationMessage(msgDescricaoEmBranco, ""));
		}
		validator.onErrorRedirectTo(this).lista();
	}


	@Get @Path("/sessao/{id}")
	public void sessao(Long id) {
		Sessao sessao = agenda.sessao(id);
		if (sessao == null) {
			result.notFound();
		}

		result.include("sessao", sessao);
	}

	@Post @Path("/sessao/{sessaoId}/reserva")
	public void reserva(Long sessaoId, final Integer quantidade) {
		Sessao sessao = agenda.sessao(sessaoId);
		if (sessao == null) {
			result.notFound();
			return;
		}

		validaReserva(quantidade, sessao);

		sessao.reserva(quantidade);
		result.include("message", msgIngressoReservado);

		result.redirectTo(IndexController.class).index();
	}

	private void validaReserva(final Integer quantidade, Sessao sessao) {
		if (quantidade < 1) {
			validator.add(new ValidationMessage(msgLugarEmBranco, ""));
		}

		if (!sessao.podeReservar(quantidade)) {
			validator.add(new ValidationMessage(msgSemIngresso, ""));
		}

		// em caso de erro, redireciona para a lista de sessao
		validator.onErrorRedirectTo(this).sessao(sessao.getId());
	}

	@Post @Path("/espetaculo/{espetaculoId}/sessoes")
	public void cadastraSessoes(Long espetaculoId, LocalDate inicio, LocalDate fim, LocalTime horario, Periodicidade periodicidade) {
		Espetaculo espetaculo = carregaEspetaculo(espetaculoId);

		// cria sessoes baseado no periodo de inicio e fim passados pelo usuario
		List<Sessao> sessoes = espetaculo.criaSessoes(inicio, fim, horario, periodicidade);

		agenda.agende(sessoes);

		result.include("message", sessoes.size() + " sessoes criadas com sucesso");
		result.redirectTo(this).lista();
	}

	private Espetaculo carregaEspetaculo(Long espetaculoId) {
		Espetaculo espetaculo = agenda.espetaculo(espetaculoId);
		validaEspetaculo(espetaculo);
		return espetaculo;
	}

	private void validaEspetaculo(Espetaculo espetaculo) {
		if (espetaculo == null) {
			validator.add(new ValidationMessage("", ""));
		}
		validator.onErrorUse(status()).notFound();
	}
}
