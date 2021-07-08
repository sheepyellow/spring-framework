/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		Set<String> processedBeans = new HashSet<>();

		/**
		 * 判断当前beanFactory是否实现了BeanDefinitionRegistry
		 */
		if (beanFactory instanceof BeanDefinitionRegistry) {
			/**
			 * 如果beanFactory实现了BeanDefinitionRegistry，则强行把beanFactory转为BeanDefinitionRegistry
			 */
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			/**
			 * 新建一个数组，用于保存BeanFactoryPostProcessor类型的后置处理器
			 */
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			/**
			 * 新建一个数组，用于保存BeanDefinitionRegistryPostProcessor类型的后置处理器
			 */
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			/**
			 * 循环遍历传入的beanFactoryPostProcessors
			 */
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				/**
				 * 判断当前postProcessor后置处理器是不是BeanDefinitionRegistryProcessor
				 */
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					/**
					 * 强制类型转换
					 */
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					/**
					 * 调用它的后置方法
					 */
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					/**
					 * 添加到我们用于保存BeanDefinitionRegistryPostProcessor的集合中
					 */
					registryProcessors.add(registryProcessor);
				}
				else {
					/**
					 * 若没有实现BeanDefinitionRegistryPostProcessor接口，那么就是BeanFactoryPostProcessor把当前的后置处理器加入到
					 * regularPostProcessors中
					 */
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			/**
			 * 定义一个集合用于保存当前准备创建的BeanDefinitionRegistryPostProcessor
			 */
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			/**
			 * 第一步：去容器中获取BeanDefinitionRegistryPostProcessor的bean的处理器名称
			 * String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false)
			 * Debug进去看到如下：为什么就一个ConfigurationClassPostProcessor，因为我们自定的BeanDefinitionRegistryPostProcessor还没有被加入到Spring容器中去
			 */
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			/**
			 * 循环上一步获取的BeanDefinitionRegistryPostProcessor的类型名称
			 */
			for (String ppName : postProcessorNames) {
				/**
				 * 判断是否实现了PriorityOrdered接口的
				 */
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					/**
					 * 显示的调用getBean()的方式获取到该对象并加入到currentRegistryProcessors集合中
					 */
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					/**
					 * 同时也加入到processedBeans集合中去
					 */
					processedBeans.add(ppName);
				}
			}
			/**
			 * 对currentRegistryProcessors集合中的BeanDefinitionRegistryPostProcessor进行排序
			 */
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			/**
			 * 把currentRegistryProcessors集合排序好之后的对象保存到registryProcessors中
			 */
			registryProcessors.addAll(currentRegistryProcessors);
			/**
			 * 在这里典型的BeanDefinitionRegistryPostProcessor就是ConfigurationClassPostProcessor
			 * 用于进行bean定义的加载，比如我们的包扫描，@import等
			 */
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			/**
			 * 调用完之后，马上clear
			 */
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			/**
			 * 下一步，又去容器中获取BeanDefinitionRegistryPostProcessor的bean的处理器名称
			 */
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			/**
			 * 循环上一步获取的BeanDefinitionRegistryPostProcessor的类型名称
			 */
			for (String ppName : postProcessorNames) {
				/**
				 * 表示没有被处理过，且实现了Ordered接口
				 * 对于上一次循环（128行），这里的区别是是否实现了Ordered接口，而上一次循环是遍历是否实现了PriorityOrdered接口
				 */
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					/**
					 * 显示的调用getBean()的方式获取出该对象然后加入到currentRegistryProcessors集合中去
					 */
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					/**
					 * 同时也加入到processedBean集合中去
					 */
					processedBeans.add(ppName);
				}
			}
			/**
			 * 对currentRegistryProcessors集合中BeanDefinitionRegistryPostProcessor进行排序
			 */
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			/**
			 * 把currentRegistryProcessors集合排序好之后的对象保存到registryProcessors中
			 */
			registryProcessors.addAll(currentRegistryProcessors);
			/**
			 * 调用他的后置处理方法
			 */
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			/**
			 * 调用完之后，马上clear
			 */
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			/**
			 * 调用没有实现任何优先级接口的BeanDefinitionRegistryPostProcessor
			 * 定义一个重复处理的开关变量，默认为true
			 */
			boolean reiterate = true;
			/**
			 * 第一次都可以进入循环
			 */
			while (reiterate) {
				/**
				 * 进入循环马上把开关变量给改为false
				 */
				reiterate = false;
				/**
				 * 从容器中获取实现了BeanDefinitionRegistryPostProcessor接口的bean的处理器名称
				 */
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				/**
				 * 循环上一步获取的BeanDefinitionRegistryPostProcessor的类型名称
				 */
				for (String ppName : postProcessorNames) {
					/**
					 * 没有被处理过的
					 */
					if (!processedBeans.contains(ppName)) {
						/**
						 * 显示的调用getBean()的方式获取出该对象然后加入到currentRegistryProcessors集合中去
						 */
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						/**
						 * 同时也加入到processedBeans集合中去
						 */
						processedBeans.add(ppName);
						/**
						 * 再次设置为true
						 */
						reiterate = true;
					}
				}
				/**
				 * 对currentRegistryProcessors集合中BeanDefinitionRegistryPostProcessor进行排序
				 */
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				/**
				 * 把他加入到用于保存到registryProcessors中
				 */
				registryProcessors.addAll(currentRegistryProcessors);
				/**
				 * 调用他的后置处理方法
				 */
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				/**
				 * 调用完之后，马上clear
				 */
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			/**
			 * 调用实现了BeanDefinitionRegistryPostProcessor，也同时实现了beanFactoryPostProcessor的方法
			 */
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			/**
			 * 调用beanFactoryPostProcessor
			 */
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			/**
			 * 如果当前beanFactory没有实现BeanDefinitionRegistry，则直接调用beanFactoryPostProcessor接口的方法进行后置处理
			 */
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		/**
		 * 最后一步，获取容器中所有的BeanFactoryPostProcessor
		 */
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		/**
		 * 保存BeanFactoryPostProcessor类型实现了PriorityOrdered
		 */
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		/**
		 * 保存BeanFactoryPostProcessor类型实现了Ordered接口的
		 */
		List<String> orderedPostProcessorNames = new ArrayList<>();
		/**
		 * 保存BeanFactoryPostProcessor没有实现任何优先级的接口的
		 */
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			/**
			 * processedBeans包含的话，表示上面处理BeanDefinitionRegistryPostProcessor的时候处理过了
			 */
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			/**
			 * 判断是否实现了PriorityOrdered
			 */
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			/**
			 * 判断是否实现了Ordered
			 */
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			/**
			 * 没有实现任何优先接口
			 */
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		/**
		 * 排序并
		 * 先调用BeanFactoryPostProcessor实现了PriorityOrdered接口的
		 */
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		/**
		 * 排序并
		 * 再调用BeanFactoryPostProcessor实现了Ordered接口的
		 */
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		/**
		 * 最后调用没有实现任何方法接口的
		 */
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	/**
	 * 往容器中注册bean的后置处理器
	 * bean的后置处理器在什么时候被调用？
	 * 在bean的生命周期中
	 * @param beanFactory
	 * @param applicationContext
	 */
	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		/**
		 * 从容器中获取所有BeanPostProcessor的bean名称
		 */
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		/**
		 * beanFactory.getBeanPostProcessorCount()获取的是已经添加到beanFactory中beanPostProcessor集合中的后置处理器的数量
		 * postProcessorNames.length是beanFactory工厂中BeanPostProcessor个数
		 * 有注册了BeanPostProcessorChecker的后置处理器
		 */
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		/**
		 * 按照BeanPostProcessor实现的优先级接口来分离我们的后置处理器
		 */
		/**
		 * 保存实现了priorityOrdered接口的BeanPostProcessor
		 */
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		/**
		 * 保存容器内部的BeanPostProcessor
		 */
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		/**
		 * 保存实现了Oredered接口的BeanPostProcessor
		 */
		List<String> orderedPostProcessorNames = new ArrayList<>();
		/**
		 * 保存没有实现任何优先级接口的BeanPostProcessor
		 */
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		/**
		 * 遍历容器中所有的postProcessor的bean名称
		 */
		for (String ppName : postProcessorNames) {
			/**
			 * 若实现了PriorityOrdered接口的
			 */
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				/**
				 * 显式的调用getBean流程创建bean的后置处理器
				 */
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				/**
				 * 加入到集合中
				 */
				priorityOrderedPostProcessors.add(pp);
				/**
				 * 判断是否实现了MergedBeanDefinitionPostProcessor
				 */
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					/**
					 * 加入到集合中
					 */
					internalPostProcessors.add(pp);
				}
			}
			/**
			 * 判断是否实现了Ordered
			 */
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		/**
		 * 把实现了PriorityOrdered的beanPostProcessor注册到容器中
		 */
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		/**
		 * 处理实现了Ordered的bean后置处理器
		 */
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			/**
			 * 显式调用getBean方法
			 */
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			/**
			 * 加入到集合中
			 */
			orderedPostProcessors.add(pp);
			/**
			 * 判断是否实现了MergedBeanDefinitionPostProcessor
			 */
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				/**
				 * 加入到集合中
				 */
				internalPostProcessors.add(pp);
			}
		}
		/**
		 * 排序并且注册实现了Order接口的后置处理器
		 */
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		/**
		 * 实例化我们所有的非排序接口的
		 */
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		/**
		 * 注册我们普通的没有实现任何排序接口的
		 */
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		/**
		 * 注册MergedBeanDefinitionPostProcessor类型的后置处理器
		 */
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		/**
		 * 注册ApplicationListenerDetector应用监听器探测器的后置处理器
		 */
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {
		/**
		 * 获取容器中的ConfigurationClassPostProcessor的后置处理器进行Bean定义扫描
		 */
		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
