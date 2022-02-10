package su.knst.moneysaver.http.routers.tags

import com.google.inject.{Inject, Singleton}
import su.knst.moneysaver.objects.Tag
import su.knst.moneysaver.public_.tables.Tags.TAGS
import su.knst.moneysaver.utils.Database

import java.util

@Singleton
class TagsDatabase @Inject()
(
  database: Database
) {
  def getUserTags(user: Int): util.List[Tag] = {
    database.context.selectFrom(TAGS)
      .where(TAGS.USER.eq(user))
      .orderBy(TAGS.ID)
      .fetch().map(r => r.into(classOf[Tag]))
  }

  def newTag(user: Int, name: String, kind: Int, limit: Double): Int = {
    database.context
      .insertInto(TAGS)
      .set(TAGS.USER, Int.box(user))
      .set(TAGS.NAME, name.trim)
      .set(TAGS.KIND, Int.box(kind))
      .set(TAGS.LIMIT, Double.box(limit))
      .returningResult(TAGS.ID)
      .fetchOne().value1()
  }

  def editTag(id: Int, name: String, kind: Int, limit: Double): Unit ={
    database.context
      .update(TAGS)
      .set(TAGS.NAME, name.trim)
      .set(TAGS.KIND, Int.box(kind))
      .set(TAGS.LIMIT, Double.box(limit))
      .where(TAGS.ID.eq(id))
      .execute()
  }

  def userOwnedTag(user: Int, tag: Int): Boolean = {
    database.context
      .select(TAGS.USER)
      .from(TAGS)
      .where(TAGS.ID.eq(tag))
      .fetchOptional().map[Boolean](_.value1() == user).orElse(false)
  }
}