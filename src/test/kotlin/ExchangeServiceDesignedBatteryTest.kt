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

       //..
        }
    }
)
