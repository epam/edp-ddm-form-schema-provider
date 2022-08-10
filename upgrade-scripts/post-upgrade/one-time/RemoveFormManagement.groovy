void call() {
    sh "helm uninstall form-management -n ${NAMESPACE} || true; "
}

return this;
