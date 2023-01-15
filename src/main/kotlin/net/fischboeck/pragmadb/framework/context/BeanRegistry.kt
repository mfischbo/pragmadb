package net.fischboeck.pragmadb.framework.context

import mu.KLogging

object BeanRegistry: KLogging() {

    private val beans = mutableMapOf<String, Any>()


    fun registerBeanDefinition(bean: Any) {
        if (!beans.containsKey(bean::class.qualifiedName)) {
            logger.debug { "Registering bean for type ${bean::class.qualifiedName!!}"}
            beans[bean::class.qualifiedName!!] = bean
        }
    }


    fun getBean(name: String): Any? {
        return beans[name]
    }
}