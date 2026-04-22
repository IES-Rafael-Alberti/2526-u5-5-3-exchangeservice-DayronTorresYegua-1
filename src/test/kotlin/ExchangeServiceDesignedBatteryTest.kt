package com.example.exchange

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import org.iesra.revilofe.ExchangeRateProvider
import org.iesra.revilofe.ExchangeService
import org.iesra.revilofe.InMemoryExchangeRateProvider
import org.iesra.revilofe.Money

class ExchangeServiceDesignedBatteryTest : DescribeSpec({

    afterTest {
        clearAllMocks()
    }

    describe("battery designed from equivalence classes for ExchangeService") {

        describe("Validacion de inputs") {
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider)

            it("Si tiene cantidad positiva y monedas de origen y destino con 3 letras devuelve la conversion") {
                every { provider.rate("USDEUR") } returns 0.92
                service.exchange(Money(1000, "USD"), "EUR") shouldBe 920

            }

            it("Lanza una excepcion si la cantidad es cero") {
                shouldThrow<IllegalArgumentException> {
                    service.exchange(Money(0, "USD"), "EUR")
                }
            }

            it("Lanza una excepcion si la cantidad negativa") {
                shouldThrow<IllegalArgumentException> {
                    service.exchange(Money(-1, "USD"), "EUR")
                }
            }

            it("Lanza una excepcion cuando el codigo de la moneda de origen no es valido") {
                shouldThrow<IllegalArgumentException> {
                    service.exchange(Money(1000, "ABCD"), "EUR")
                }
            }

            it("Lanza una excecion cuando el codigo de la moneda objetivo no es valido") {
                shouldThrow<IllegalArgumentException> {
                    service.exchange(Money(1000, "USD"), "WXYZ")
                }
            }
        }

        describe("Relación entre moneda origen y destino") {

            it("Devolver misma cantidad si origen y destino son iguales") {
                val realProvider = InMemoryExchangeRateProvider(mapOf("USDEUR" to 0.92))
                val providerSpy = spyk(realProvider)
                val service = ExchangeService(providerSpy)

                service.exchange(Money(1000, "USD"), "USD") shouldBe 1000

                verify(exactly = 0) { providerSpy.rate(any()) }
                }

            it("Origen y destino distinto con tasa directa") {
                val provider = mockk<ExchangeRateProvider>()
                every { provider.rate("USDEUR") } returns 0.92
                val service = ExchangeService(provider)

                service.exchange(Money(1000, "USD"), "EUR") shouldBe 920
            }

            it("Origen y destino distinto sin tasa directa pero con ruta cruzada valida") {
                val provider = mockk<ExchangeRateProvider>()
                every { provider.rate("USDEUR") } throws IllegalArgumentException("sin ruta")
                every { provider.rate("USDGBP") } returns 0.79
                every { provider.rate("GBPEUR") } returns 1.16
                val service = ExchangeService(provider, supportedCurrencies = setOf("USD", "EUR", "GBP", "JPY"))

                service.exchange(Money(1000, "USD"), "EUR") shouldBe (1000 * 0.79 * 1.16).toLong()
            }

            it("Origen y destino distinto sin ninguna ruta posible") {
                val provider = mockk<ExchangeRateProvider>()
                every { provider.rate(any()) } throws IllegalArgumentException("sin ruta")
                val service = ExchangeService(provider, supportedCurrencies = setOf("USD", "EUR", "GBP", "JPY"))

                shouldThrow<IllegalArgumentException> {
                    service.exchange(Money(1000, "USD"), "EUR")
                }
            }
        }
        describe("Estrategia de busqueda de tasas") {
            it("Exito en consulta directa") {
                val realProvider = InMemoryExchangeRateProvider(mapOf("USDEUR" to 0.92))
                val providerSpy = spyk(realProvider)
                val service = ExchangeService(providerSpy)

                service.exchange(Money(1000, "USD"), "EUR") shouldBe 920

                verify(exactly = 1) { providerSpy.rate("USDEUR") }
            }

            it("Fallo en consulta directa y éxito en primer cruce válido") {
                val provider = mockk<ExchangeRateProvider>()
                every { provider.rate("GBPJPY") } throws IllegalArgumentException("sin ruta")
                every { provider.rate("GBPUSD") } returns 1.27
                every { provider.rate("USDJPY") } returns 150.5
                val service = ExchangeService(provider, supportedCurrencies = setOf("USD", "GBP", "JPY"))

                service.exchange(Money(2, "GBP"), "JPY") shouldBe (2 * 1.27 * 150.5).toLong()

                verifySequence {
                    provider.rate("GBPJPY")
                    provider.rate("GBPUSD")
                    provider.rate("USDJPY")
                }
            }

            it("Fallo en primer cruce y éxito en un cruce alternativo posterior") {
                val provider = mockk<ExchangeRateProvider>()
                every { provider.rate("GBPJPY") } throws IllegalArgumentException("sin ruta")
                every { provider.rate("GBPEUR") } throws IllegalArgumentException("sin ruta")
                every { provider.rate("EURJPY") } throws IllegalArgumentException("sin ruta")
                every { provider.rate("GBPUSD") } returns 1.27
                every { provider.rate("USDJPY") } returns 150.5
                val service = ExchangeService(provider, supportedCurrencies = setOf("USD", "EUR", "GBP", "JPY"))

                service.exchange(Money(2, "GBP"), "JPY") shouldBe (2 * 1.27 * 150.5).toLong()
            }

            it("Fallo en todas las consultas") {
                val provider = mockk<ExchangeRateProvider>()
                every { provider.rate(any()) } throws IllegalArgumentException("sin ruta")
                val service = ExchangeService(provider, supportedCurrencies = setOf("USD", "EUR", "GBP", "JPY"))

                shouldThrow<IllegalArgumentException> {
                    service.exchange(Money(1000, "USD"), "EUR")
                }
            }
        }
        }
    }
)
