@freezed
sealed class OrderShelf with _$OrderShelf {

  const factory OrderShelf({
    @Default('') @JsonKey(name: "id") String id,
    @Default('') String title,
    @Default('') String? name
  }) = _OrderShelf;


}



