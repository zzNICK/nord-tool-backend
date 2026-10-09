#!/usr/bin/env bash
# Gate da política de testes e da convenção de serviços (steering/nord-tool-backend do nord-center-infra-agent).
# Falha se aparecer teste fora de *ServiceImplTest / guard/, *ServiceImplTest sem classe correspondente,
# @SpringBootTest/@WebMvcTest/@JdbcTest fora das guardas, *Service concreto ou *ServiceImpl sem interface.
set -euo pipefail
cd "$(dirname "$0")/.."

erros=0
falha() { echo "::error::$1"; erros=$((erros + 1)); }

GUARDAS="src/test/java/br/com/nord_tool_backend/guard/RotasProtegidasGuardTest.java src/test/java/br/com/nord_tool_backend/guard/ContratoSegurancaGuardTest.java"

while IFS= read -r arquivo; do
  case "$arquivo" in
    *ServiceImplTest.java) ;;
    */support/*) ;;
    *)
      permitido=0
      for g in $GUARDAS; do [ "$arquivo" = "$g" ] && permitido=1; done
      [ "$permitido" -eq 1 ] || falha "Teste fora da política: $arquivo (só *ServiceImplTest, guard/ e support/)"
      ;;
  esac
done < <(find src/test/java -name '*.java' | sort)

while IFS= read -r teste; do
  classe=$(basename "$teste" Test.java)
  find src/main/java -name "$classe.java" | grep -q . || falha "Sem classe correspondente em src/main: $teste"
done < <(find src/test/java -name '*ServiceImplTest.java' | sort)

while IFS= read -r arquivo; do
  case " $GUARDAS " in *" $arquivo "*) ;; *) falha "Contexto Spring/integração fora das guardas: $arquivo" ;; esac
done < <(grep -rlE '@(SpringBootTest|WebMvcTest|JdbcTest|DataJdbcTest)' src/test/java || true)

while IFS= read -r arquivo; do
  nome=$(basename "$arquivo" .java)
  grep -qE "interface ${nome}\b" "$arquivo" || falha "*Service deve ser interface: $arquivo"
done < <(find src/main/java -name '*Service.java' | sort)

while IFS= read -r arquivo; do
  nome=$(basename "$arquivo" .java)
  grep -qE "class ${nome} implements [A-Za-z]+Service\b" "$arquivo" || falha "*ServiceImpl deve implementar um *Service: $arquivo"
done < <(find src/main/java -name '*ServiceImpl.java' | sort)

if [ "$erros" -gt 0 ]; then
  echo "Política de testes/serviços violada ($erros problema(s))."
  exit 1
fi
echo "Política de testes e convenção de serviços OK."
