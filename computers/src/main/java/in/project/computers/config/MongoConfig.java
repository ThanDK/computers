package in.project.computers.config;

import org.bson.types.Decimal128;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new DoubleToDecimal128Converter());
        return new MongoCustomConversions(converters);
    }

    private static class DoubleToDecimal128Converter implements Converter<Double, Decimal128> {
        @Override
        public Decimal128 convert(Double source) {
            return new Decimal128(source.longValue());
        }
    }
}