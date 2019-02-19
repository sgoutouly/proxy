package io.commare.recorder.proxy;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


public class PersistenceTest {

    @Test
    public void shouldSaveExchange() {

        final String question = "commareaQuestion";
        final ByteBuf q = Utils.encodeCics(question);
        final String reponse = "commareaReponse";
        final ByteBuf r = Utils.encodeCics(reponse);

        Bucket mockedBucket = Mockito.mock(Bucket.class);
        List<JsonDocument> result  = new ArrayList<>();

        Mockito.when(mockedBucket.upsert(Mockito.any(JsonDocument.class)))
            .then(i -> {
                final JsonDocument jd = i.getArgumentAt(0, JsonDocument.class);
                result.add(jd);
                return jd;
            });

        Assert.assertEquals(r, new Engine(2000, "locahost", 3000, mockedBucket).saveExchange(q, r));
        Assert.assertNotNull(result.get(0));
        Assert.assertEquals(toBase64(q), result.get(0).id());
        Assert.assertEquals(Utils.decodeCics(q), result.get(0).content().get("questionDecoded"));
        Assert.assertEquals(Utils.decodeCics(r), result.get(0).content().get("reponseDecoded"));
        Assert.assertEquals(toBase64(r), result.get(0).content().get("reponse"));
        Assert.assertEquals(toBase64(q), result.get(0).content().get("question"));
    }

    private String toBase64(final ByteBuf buf) {
        return new String(Base64.getEncoder().encode(ByteBufUtil.getBytes(buf)), StandardCharsets.UTF_8);
    }
}
