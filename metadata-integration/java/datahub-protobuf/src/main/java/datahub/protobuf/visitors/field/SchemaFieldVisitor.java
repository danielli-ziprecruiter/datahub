package datahub.protobuf.visitors.field;

import com.linkedin.schema.SchemaField;
import com.linkedin.util.Pair;
import datahub.protobuf.model.ProtobufField;
import datahub.protobuf.ProtobufUtils;
import datahub.protobuf.visitors.ProtobufModelVisitor;
import datahub.protobuf.visitors.VisitContext;

import java.util.stream.Stream;
import java.util.Map;
import java.util.HashMap;

public class SchemaFieldVisitor implements ProtobufModelVisitor<Pair<SchemaField, Double>> {

    @Override
    public Stream<Pair<SchemaField, Double>> visitField(ProtobufField field, VisitContext context) {
        return context.streamAllPaths(field).map(path ->
                Pair.of(
                        new SchemaField()
                                .setFieldPath(context.getFieldPath(path))
                                .setNullable(true)
                                .setDescription(getDescription(field))
                                .setNativeDataType(field.nativeType())
                                .setType(field.schemaFieldDataType()),
                        context.calculateSortOrder(path, field)));
    }

    static String getDescription(ProtobufField field) {
        Map<String, Object> byNameMap = new HashMap<>();
        if (field.getNativeType().equals("oneof")) {
            field.oneOfProto().getOptions().getAllFields().forEach((k, v) -> byNameMap.put(k.getName(), v));
        } else {
            ProtobufUtils.getFieldOptions(field.getFieldProto()).stream().forEach(pair -> byNameMap.put(pair.getKey().getName(), pair.getValue()));
        }
        return byNameMap.getOrDefault("description", "").toString();
    }
}
